package com.jper.flycat.core.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

/**
 * SSL工厂类
 *
 * @author ywxiang
 * @date 2020/12/29 下午9:01
 */
@Slf4j
public class ContextSslFactory {
    private static final SSLContext SSL_CONTEXT_S;
    private static final SSLContext SSL_CONTEXT_C;

    static {
        SSLContext sslContext1 = null;
        SSLContext sslContext2 = null;

        try {
            sslContext1 = SSLContext.getInstance("SSLv3");
            sslContext2 = SSLContext.getInstance("SSLv3");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            if (sslContext1 != null && getKeyManagersServer() != null && getTrustManagersServer() != null) {
                sslContext1.init(getKeyManagersServer(), getTrustManagersClient(), null);
            }
            if (sslContext2 != null && getKeyManagersClient() != null && getTrustManagersClient() != null) {
                sslContext2.init(getKeyManagersClient(), getTrustManagersClient(), null);
            }
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        if (sslContext1 != null && sslContext2 != null) {
            sslContext1.createSSLEngine().getSupportedCipherSuites();
            sslContext2.createSSLEngine().getSupportedCipherSuites();
        }
        SSL_CONTEXT_S = sslContext1;
        SSL_CONTEXT_C = sslContext2;
    }

    public ContextSslFactory() {
    }

    public static SSLContext getSslContext1() {
        return SSL_CONTEXT_S;
    }

    public static SSLContext getSslContext2() {
        return SSL_CONTEXT_C;
    }

    private static TrustManager[] getTrustManagersServer() {
        FileInputStream is = null;
        KeyStore ks = null;
        TrustManagerFactory keyFac = null;

        TrustManager[] kms = null;
        try {
            // 获得KeyManagerFactory对象. 初始化位默认算法
            keyFac = TrustManagerFactory.getInstance("SunX509");
            is = new FileInputStream((new ClassPathResource("ssl/sChat.jks")).getFile());
            ks = KeyStore.getInstance("JKS");
            String keyStorePass = "sNetty";
            ks.load(is, keyStorePass.toCharArray());
            keyFac.init(ks);
            kms = keyFac.getTrustManagers();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return kms;
    }

    private static TrustManager[] getTrustManagersClient() {
        FileInputStream is = null;
        KeyStore ks = null;
        TrustManagerFactory keyFac = null;

        TrustManager[] kms = null;
        try {
            // 获得KeyManagerFactory对象. 初始化位默认算法
            keyFac = TrustManagerFactory.getInstance("SunX509");
            is = new FileInputStream((new ClassPathResource("ssl/cChat.jks")).getFile());
            ks = KeyStore.getInstance("JKS");
            String keyStorePass = "sNetty";
            ks.load(is, keyStorePass.toCharArray());
            keyFac.init(ks);
            kms = keyFac.getTrustManagers();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return kms;
    }

    private static KeyManager[] getKeyManagersServer() {
        FileInputStream is = null;
        KeyStore ks = null;
        KeyManagerFactory keyFac = null;

        KeyManager[] kms = null;
        try {
            // 获得KeyManagerFactory对象. 初始化位默认算法
            keyFac = KeyManagerFactory.getInstance("SunX509");
            is = new FileInputStream((new ClassPathResource("ssl/sChat.jks")).getFile());
            ks = KeyStore.getInstance("JKS");
            String keyStorePass = "sNetty";
            ks.load(is, keyStorePass.toCharArray());
            keyFac.init(ks, keyStorePass.toCharArray());
            kms = keyFac.getKeyManagers();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return kms;
    }

    private static KeyManager[] getKeyManagersClient() {
        FileInputStream is = null;
        KeyStore ks = null;
        KeyManagerFactory keyFac = null;

        KeyManager[] kms = null;
        try {
            // 获得KeyManagerFactory对象. 初始化位默认算法
            keyFac = KeyManagerFactory.getInstance("SunX509");
            is = new FileInputStream((new ClassPathResource("ssl/cChat.jks")).getFile());
            ks = KeyStore.getInstance("JKS");
            String keyStorePass = "sNetty";
            ks.load(is, keyStorePass.toCharArray());
            keyFac.init(ks, keyStorePass.toCharArray());
            kms = keyFac.getKeyManagers();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return kms;
    }
}
