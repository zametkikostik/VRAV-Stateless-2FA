package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Detects MetaMask on device and opens it.
 * Full personal_sign round-trip needs WalletConnect (network + Project ID).
 * Local web3j signing remains the offline path when the user provides a key.
 */
object MetaMaskHelper {

    const val METAMASK_PACKAGE = "io.metamask"
    private const val TAG = "MetaMaskHelper"

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(METAMASK_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (t: Throwable) {
            Log.w(TAG, "isInstalled check failed: ${t.message}")
            false
        }
    }

    /** Opens MetaMask if installed; otherwise opens Play Store listing. */
    fun openMetaMask(context: Context): Boolean {
        return try {
            if (isInstalled(context)) {
                val launch = context.packageManager.getLaunchIntentForPackage(METAMASK_PACKAGE)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launch)
                    return true
                }
            }
            val market = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$METAMASK_PACKAGE")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(market)
            } catch (_: ActivityNotFoundException) {
                val web = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$METAMASK_PACKAGE")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(web)
            }
            false
        } catch (t: Throwable) {
            Log.e(TAG, "openMetaMask failed", t)
            false
        }
    }

    fun openMetaMaskDeepLink(context: Context): Boolean {
        return try {
            val uri = Uri.parse("https://metamask.app.link/")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isInstalled(context)) {
                    setPackage(METAMASK_PACKAGE)
                }
            }
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "deep link failed, falling back to launch: ${t.message}")
            openMetaMask(context)
        }
    }
}
