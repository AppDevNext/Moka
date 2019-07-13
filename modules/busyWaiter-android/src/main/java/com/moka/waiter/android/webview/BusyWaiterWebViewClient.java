package com.moka.waiter.android.webview;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import com.moka.waiter.BusyWaiter;

import static com.moka.waiter.BusyWaiter.Category.NETWORK;

@SuppressWarnings("deprecation") // need to delegate to deprecated methods.
public class BusyWaiterWebViewClient extends WebViewClient {

    private final BusyWaiter busyWaiter;
    private final WebViewClient delegate;

    private BusyWaiterWebViewClient(final BusyWaiter busyWaiter, final WebViewClient delegate) {
        this.busyWaiter = busyWaiter;
        this.delegate = delegate;
    }

    public static class Builder {
        private BusyWaiter busyWaiter1;
        private WebViewClient delegate;

        public Builder(final BusyWaiter busyWaiter) {
            this.busyWaiter1 = busyWaiter;
        }

        public Builder wrapWebViewClient(final WebViewClient delegate) {
            this.delegate = delegate;
            return this;
        }

        public BusyWaiterWebViewClient build() {
            return new BusyWaiterWebViewClient(busyWaiter1, delegate);
        }
    }

    public static Builder with(BusyWaiter busyWaiter) {
        return new Builder(busyWaiter);
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        busyWaiter.busyWith(view, NETWORK);
        delegate.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        delegate.onPageFinished(view, url);
        busyWaiter.completed(view);
    }

    @Override
    public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
        delegate.onReceivedError(view, errorCode, description, failingUrl);
        busyWaiter.completed(view);
    }

    @Override
    public void onReceivedSslError(final WebView view, final SslErrorHandler handler, final SslError error) {
        delegate.onReceivedSslError(view, handler, error);
        busyWaiter.completed(view);
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        return delegate.shouldOverrideUrlLoading(view, url);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
        return delegate.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onLoadResource(final WebView view, final String url) {
        delegate.onLoadResource(view, url);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onPageCommitVisible(final WebView view, final String url) {
        delegate.onPageCommitVisible(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
        return delegate.shouldInterceptRequest(view, url);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, final WebResourceRequest request) {
        return delegate.shouldInterceptRequest(view, request);
    }

    @Override
    public void onTooManyRedirects(final WebView view, final Message cancelMsg, final Message continueMsg) {
        delegate.onTooManyRedirects(view, cancelMsg, continueMsg);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(final WebView view, final WebResourceRequest request, final WebResourceError error) {
        delegate.onReceivedError(view, request, error);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onReceivedHttpError(final WebView view, final WebResourceRequest request, final WebResourceResponse errorResponse) {
        delegate.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onFormResubmission(final WebView view, final Message dontResend, final Message resend) {
        delegate.onFormResubmission(view, dontResend, resend);
    }

    @Override
    public void doUpdateVisitedHistory(final WebView view, final String url, final boolean isReload) {
        delegate.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onReceivedClientCertRequest(final WebView view, final ClientCertRequest request) {
        delegate.onReceivedClientCertRequest(view, request);
    }

    @Override
    public void onReceivedHttpAuthRequest(final WebView view, final HttpAuthHandler handler, final String host, final String realm) {
        delegate.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    @Override
    public boolean shouldOverrideKeyEvent(final WebView view, final KeyEvent event) {
        return delegate.shouldOverrideKeyEvent(view, event);
    }

    @Override
    public void onUnhandledKeyEvent(final WebView view, final KeyEvent event) {
        delegate.onUnhandledKeyEvent(view, event);
    }

    @Override
    public void onScaleChanged(final WebView view, final float oldScale, final float newScale) {
        delegate.onScaleChanged(view, oldScale, newScale);
    }

    @Override
    public void onReceivedLoginRequest(final WebView view, final String realm, final String account, final String args) {
        delegate.onReceivedLoginRequest(view, realm, account, args);
    }
}
