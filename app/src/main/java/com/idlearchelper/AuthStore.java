package com.idlearchelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Only the API token is stored. The password and email are never persisted. */
final class AuthStore {
    private static final String ALIAS = "idle_arc_helper_auth_v1";
    private static final String PREFS = "api_auth";
    private static SharedPreferences prefs() { return App.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    private static KeyStore keyStore() throws Exception { KeyStore s = KeyStore.getInstance("AndroidKeyStore"); s.load(null); return s; }
    private static SecretKey key(boolean create) throws Exception {
        KeyStore store = keyStore();
        SecretKey key = (SecretKey) store.getKey(ALIAS, null);
        if (key == null && create) {
            KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            key = gen.generateKey();
        }
        return key;
    }
    static synchronized void save(String token) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(true));
        byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        if (!prefs().edit().putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString("token", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
            throw new java.io.IOException("Не удалось сохранить сессию на устройстве.");
    }
    static synchronized String load() throws Exception {
        String iv = prefs().getString("iv", null), encrypted = prefs().getString("token", null);
        if (iv == null || encrypted == null) return null;
        SecretKey key = key(false);
        if (key == null) throw new java.io.IOException("Не удалось открыть сессию. Войдите заново.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
    static synchronized void clear() throws Exception {
        if (!prefs().edit().clear().commit()) throw new java.io.IOException("Не удалось удалить сессию.");
        KeyStore store = keyStore(); if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS);
    }
}
