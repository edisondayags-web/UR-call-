package com.urcall.app.webrtc

import android.content.Context
import org.webrtc.*

/**
 * Ito na yung tunay na "UR call" engine - dito nangyayari yung audio connection.
 *
 * STUN = tumutulong makahanap ng public IP (libre, galing Google, laging naka-on).
 * TURN = ito yung "ako na bahala sa internet connection mo" - kapag hindi
 *        pwedeng mag-direct connection ang dalawang phone (madalas mangyari sa
 *        mobile data / carrier NAT), dito dadaan ang boses nila, parang relay.
 *
 * WALANG kasamang TURN credentials dito by default - kailangan mong maglagay ng
 * sarili mong TURN server (hal. metered.ca may libreng 20GB/month tier) sa
 * TURN_SERVER_URL / USERNAME / PASSWORD sa baba. Kung STUN lang, gagana sa
 * karamihan pero minsan hindi makaka-connect pag parehong strict yung network.
 */
class WebRTCClient(context: Context) {

    companion object {
        private const val STUN_SERVER = "stun:stun.l.google.com:19302"

        // TODO: palitan mo 'to ng sarili mong TURN server para guaranteed maka-connect
        // kahit gaano ka-restrictive ang network ng dalawang tumatawag.
        private const val TURN_SERVER_URL = ""
        private const val TURN_USERNAME = ""
        private const val TURN_PASSWORD = ""
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

/** Simpleng adapter para hindi na kailangan i-override lahat ng SdpObserver methods paulit-ulit. */
open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
