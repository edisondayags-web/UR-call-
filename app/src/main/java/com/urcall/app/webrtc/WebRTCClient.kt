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
}

open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
