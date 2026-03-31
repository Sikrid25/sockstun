/*
 ============================================================================
 Name        : SocksTunProvider.java
 Author      : pnut1337
 Copyright   : Copyright (c) 2026
 Description : Content Provider for external control
 ============================================================================
 */

package hev.sockstun;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;

public class SocksTunProvider extends ContentProvider {
	public static final String AUTHORITY = "hev.sockstun.provider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	private static final int URI_CONFIG = 1;
	private static final int URI_STATUS = 2;

	private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		uriMatcher.addURI(AUTHORITY, "config", URI_CONFIG);
		uriMatcher.addURI(AUTHORITY, "status", URI_STATUS);
	}

	private Preferences prefs;

	@Override
	public boolean onCreate() {
		prefs = new Preferences(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		switch (uriMatcher.match(uri)) {
			case URI_CONFIG:
				return queryConfig();
			case URI_STATUS:
				return queryStatus();
			default:
				return null;
		}
	}

	private Cursor queryConfig() {
		String[] columns = {
			"SocksAddr", "SocksUdpAddr", "SocksPort",
			"SocksUser", "SocksPass",
			"DnsIpv4", "DnsIpv6",
			"Ipv4", "Ipv6", "Global",
			"UdpInTcp", "RemoteDNS"
		};
		MatrixCursor cursor = new MatrixCursor(columns);
		cursor.addRow(new Object[] {
			prefs.getSocksAddress(),
			prefs.getSocksUdpAddress(),
			prefs.getSocksPort(),
			prefs.getSocksUsername(),
			prefs.getSocksPassword(),
			prefs.getDnsIpv4(),
			prefs.getDnsIpv6(),
			prefs.getIpv4() ? 1 : 0,
			prefs.getIpv6() ? 1 : 0,
			prefs.getGlobal() ? 1 : 0,
			prefs.getUdpInTcp() ? 1 : 0,
			prefs.getRemoteDns() ? 1 : 0
		});
		return cursor;
	}

	private Cursor queryStatus() {
		String[] columns = { "Enable" };
		MatrixCursor cursor = new MatrixCursor(columns);
		cursor.addRow(new Object[] {
			isVpnActive() ? 1 : 0
		});
		return cursor;
	}

	private boolean isVpnActive() {
		ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		Network[] networks = cm.getAllNetworks();
		for (Network network : networks) {
			NetworkCapabilities caps = cm.getNetworkCapabilities(network);
			if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
				return true;
		}
		return false;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (uriMatcher.match(uri) != URI_CONFIG)
			return 0;

		if (isVpnActive())
			return 0;

		int count = 0;

		if (values.containsKey("SocksAddr")) {
			prefs.setSocksAddress(values.getAsString("SocksAddr"));
			count++;
		}
		if (values.containsKey("SocksUdpAddr")) {
			prefs.setSocksUdpAddress(values.getAsString("SocksUdpAddr"));
			count++;
		}
		if (values.containsKey("SocksPort")) {
			prefs.setSocksPort(values.getAsInteger("SocksPort"));
			count++;
		}
		if (values.containsKey("SocksUser")) {
			prefs.setSocksUsername(values.getAsString("SocksUser"));
			count++;
		}
		if (values.containsKey("SocksPass")) {
			prefs.setSocksPassword(values.getAsString("SocksPass"));
			count++;
		}
		if (values.containsKey("DnsIpv4")) {
			prefs.setDnsIpv4(values.getAsString("DnsIpv4"));
			count++;
		}
		if (values.containsKey("DnsIpv6")) {
			prefs.setDnsIpv6(values.getAsString("DnsIpv6"));
			count++;
		}
		if (values.containsKey("Ipv4")) {
			prefs.setIpv4(values.getAsInteger("Ipv4") != 0);
			count++;
		}
		if (values.containsKey("Ipv6")) {
			prefs.setIpv6(values.getAsInteger("Ipv6") != 0);
			count++;
		}
		if (values.containsKey("Global")) {
			prefs.setGlobal(values.getAsInteger("Global") != 0);
			count++;
		}
		if (values.containsKey("UdpInTcp")) {
			prefs.setUdpInTcp(values.getAsInteger("UdpInTcp") != 0);
			count++;
		}
		if (values.containsKey("RemoteDNS")) {
			prefs.setRemoteDns(values.getAsInteger("RemoteDNS") != 0);
			count++;
		}

		if (count > 0)
			getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		Context context = getContext();
		Bundle result = new Bundle();

		if ("connect".equals(method)) {
			if (!isVpnActive()) {
				Intent intent = new Intent(context, TProxyService.class);
				intent.setAction(TProxyService.ACTION_CONNECT);
				context.startService(intent);
				result.putBoolean("success", true);
			} else {
				result.putBoolean("success", false);
				result.putString("error", "already connected");
			}
		} else if ("disconnect".equals(method)) {
			if (isVpnActive()) {
				Intent intent = new Intent(context, TProxyService.class);
				intent.setAction(TProxyService.ACTION_DISCONNECT);
				context.startService(intent);
				result.putBoolean("success", true);
			} else {
				result.putBoolean("success", false);
				result.putString("error", "already disconnected");
			}
		} else {
			result.putBoolean("success", false);
			result.putString("error", "unknown method");
		}

		return result;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
			case URI_CONFIG:
				return "vnd.android.cursor.item/vnd.hev.sockstun.config";
			case URI_STATUS:
				return "vnd.android.cursor.item/vnd.hev.sockstun.status";
			default:
				return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
}
