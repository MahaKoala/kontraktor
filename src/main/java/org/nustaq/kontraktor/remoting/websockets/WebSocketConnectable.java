package org.nustaq.kontraktor.remoting.websockets;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.net.URISyntaxException;

/**
 * Created by ruedi on 19/05/15.
 *
 * describes a remotactor connectable via websockets
 *
 */
public class WebSocketConnectable implements ConnectableActor {

    Class clz;
    String url;
    Coding coding = new Coding(SerializerType.FSTSer);

    public WebSocketConnectable() {}

    public WebSocketConnectable(Class clz, String url) {
        this.clz = clz;
        this.url = url;
    }

    public WebSocketConnectable url(String url) {
        this.url = url;
        return this;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        Promise result = new Promise();
        Runnable connect = () -> {
            JSR356ClientConnector client = null;
            try {
                client = new JSR356ClientConnector(url);
                ActorClient connector = new ActorClient(client,clz,coding);
                connector.connect().then(result);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                result.reject(e);
            }
        };
        if ( ! Actor.inside() ) {
            JSR356ClientConnector.get().execute(() -> Thread.currentThread().setName("singleton remote client actor polling"));
            JSR356ClientConnector.get().execute(connect);
        }
        else
            connect.run();
        return result;
    }

    @Override
    public WebSocketConnectable actorClass(Class actorClz) {
        clz = actorClz;
        return this;
    }

    public WebSocketConnectable coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    public WebSocketConnectable serType(SerializerType sertype) {
        this.coding = new Coding(sertype);
        return this;
    }

    public Class getClz() {
        return clz;
    }

    public String getUrl() {
        return url;
    }

    public Coding getCoding() {
        return coding;
    }
}
