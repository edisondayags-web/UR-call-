package com.urcall.app.webrtc

import android.content.Context
import org.webrtc.*

class WebRTCClient(context: Context) {

    companion object {
        private const val STUN_SERVER = "stun:stun.l.google.com:19302"
        private const val TURN_SERVER_URL = "turn:standard.relay.metered.ca:80"
        private const val TURN_USERNAME = "202fa5c74a1d43fa246a489f"
        private const val TURN_PASSWORD = "QuEhwBfVSQGuUP1I"
    }

    private val eglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null

    // ---- Echo Test (local loopback, no signaling/TURN needed) ----
    private var echoPc1: PeerConnection? = null
    private var echoPc2: PeerConnection? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    private fun iceServers(): List<PeerConnection.IceServer> {
        val servers = mutableListOf(
            PeerConnection.IceServer.builder(STUN_SERVER).createIceServer()
        )
        if (TURN_SERVER_URL.isNotBlank()) {
            servers.add(
                PeerConnection.IceServer.builder(TURN_SERVER_URL)
                    .setUsername(TURN_USERNAME)
                    .setPassword(TURN_PASSWORD)
                    .createIceServer()
            )
            servers.add(
                PeerConnection.IceServer.builder("turn:standard.relay.metered.ca:80?transport=tcp")
                    .setUsername(TURN_USERNAME)
                    .setPassword(TURN_PASSWORD)
                    .createIceServer()
            )
            servers.add(
                PeerConnection.IceServer.builder("turns:standard.relay.metered.ca:443?transport=tcp")
                    .setUsername(TURN_USERNAME)
                    .setPassword(TURN_PASSWORD)
                    .createIceServer()
            )
        }
        return servers
    }

    fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
        return peerConnection
    }

    fun addLocalAudioTrack() {
        val audioConstraints = MediaConstraints()
        val audioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("URCALL_AUDIO", audioSource)
        peerConnection?.addTrack(localAudioTrack)
    }

    fun createOffer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                peerConnection?.setLocalDescription(SdpAdapter(), sdp)
                callback(sdp)
            }
        }, constraints)
    }

    fun createAnswer(callback: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                peerConnection?.setLocalDescription(SdpAdapter(), sdp)
                callback(sdp)
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpAdapter(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun endCall() {
        peerConnection?.close()
        peerConnection = null
    }

    /**
     * Starts a local loopback "echo test": creates two PeerConnections inside
     * this same app instance, wires their SDP/ICE directly to each other
     * (no Firebase signaling, no TURN needed), and streams your mic audio
     * from pc1 -> pc2. If your mic/audio pipeline works, you will hear your
     * own voice come back through the speaker.
     */
    fun startEchoTest(onStatus: (String) -> Unit) {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(PeerConnection.IceServer.builder(STUN_SERVER).createIceServer())
        ).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }

        val pc2Observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                echoPc1?.addIceCandidate(candidate)
            }
            override fun onAddStream(stream: MediaStream?) {
                onStatus("Echo: audio track connected, you should hear yourself")
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                onStatus("Echo ICE state: $state")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                onStatus("Echo: remote track added, playback should start")
            }
        }

        val pc1Observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                echoPc2?.addIceCandidate(candidate)
            }
            override fun onAddStream(stream: MediaStream?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        }

        echoPc1 = peerConnectionFactory.createPeerConnection(rtcConfig, pc1Observer)
        echoPc2 = peerConnectionFactory.createPeerConnection(rtcConfig, pc2Observer)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val localAudioTrack = peerConnectionFactory.createAudioTrack("ECHO_AUDIO", audioSource)
        echoPc1?.addTrack(localAudioTrack)

        val constraints = MediaConstraints()
        echoPc1?.createOffer(object : SdpAdapter() {
            override fun onCreateSuccess(offerSdp: SessionDescription?) {
                offerSdp ?: return
                echoPc1?.setLocalDescription(SdpAdapter(), offerSdp)
                echoPc2?.setRemoteDescription(SdpAdapter(), offerSdp)

                echoPc2?.createAnswer(object : SdpAdapter() {
                    override fun onCreateSuccess(answerSdp: SessionDescription?) {
                        answerSdp ?: return
                        echoPc2?.setLocalDescription(SdpAdapter(), answerSdp)
                        echoPc1?.setRemoteDescription(SdpAdapter(), answerSdp)
                        onStatus("Echo test started — speak now and listen")
                    }
                }, constraints)
            }
        }, constraints)
    }

    fun stopEchoTest() {
        echoPc1?.close()
        echoPc2?.close()
        echoPc1 = null
        echoPc2 = null
    }
}
// ---- Firebase Loopback Test (real signaling path, two peers, one phone) ----
    private var loopPc1: PeerConnection? = null
    private var loopPc2: PeerConnection? = null

    fun startFirebaseLoopbackTest(
        signalingCaller: com.urcall.app.webrtc.SignalingManager,
        signalingCallee: com.urcall.app.webrtc.SignalingManager,
        onStatus: (String) -> Unit
    ) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        // Caller side peer connection
        loopPc1 = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                signalingCaller.sendIceCandidate(candidate, isCaller = true)
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                onStatus("Caller ICE state: $state")
            }
            override fun onAddStream(stream: MediaStream?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })

        // Callee side peer connection
        loopPc2 = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                signalingCallee.sendIceCandidate(candidate, isCaller = false)
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                onStatus("Callee ICE state: $state")
            }
            override fun onAddStream(stream: MediaStream?) {
                onStatus("Loopback: audio connected, dapat marinig mo na")
            }
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                onStatus("Loopback: remote track added")
            }
        })

        // Audio track from mic goes into the caller side
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val localAudioTrack = peerConnectionFactory.createAudioTrack("LOOPBACK_AUDIO", audioSource)
        loopPc1?.addTrack(localAudioTrack)

        // Listen for the answer on the caller side
        signalingCaller.listenForAnswer { answer -> loopPc1?.setRemoteDescription(SdpAdapter(), answer) }
        signalingCaller.listenForCandidates(isCaller = true) { candidate -> loopPc1?.addIceCandidate(candidate) }

        // Listen for the offer on the callee side, then answer it
        signalingCallee.listenForOffer { offer ->
            loopPc2?.setRemoteDescription(SdpAdapter(), offer)
            val constraints = MediaConstraints()
            loopPc2?.createAnswer(object : SdpAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp ?: return
                    loopPc2?.setLocalDescription(SdpAdapter(), sdp)
                    signalingCallee.sendAnswer(sdp)
                }
            }, constraints)
        }
        signalingCallee.listenForCandidates(isCaller = false) { candidate -> loopPc2?.addIceCandidate(candidate) }

        // Kick off: caller creates and sends the offer
        val constraints = MediaConstraints()
        loopPc1?.createOffer(object : SdpAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                loopPc1?.setLocalDescription(SdpAdapter(), sdp)
                signalingCaller.sendOffer(sdp)
                onStatus("Loopback test started — kumokonekta na...")
            }
        }, constraints)
    }

    fun stopFirebaseLoopbackTest() {
        loopPc1?.close()
        loopPc2?.close()
        loopPc1 = null
        loopPc2 = null
    }
open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
