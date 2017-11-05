package com.astatus.easysocketlan;

import com.astatus.easysocketlan.listener.ILanSocketDisconnectListener;
import com.astatus.easysocketlan.listener.ISocketListener;

import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2017/10/22.
 */

public class LanSocket {
    private PacketHandlerManager mHandlerMgr;

    private Observable<Packet> mReadObservale;
    private Observable<Integer> mWriteObservale;

    private ISocketListener mSocketListener;

    private ILanSocketDisconnectListener mDisconnectListener;

    private Socket mSocket;

    private String mName = "";

    private LinkedBlockingQueue<Packet> mWaitPackets = new LinkedBlockingQueue<>(100);;


    LanSocket(PacketHandlerManager handlerManager,
              ISocketListener socketListener,
              ILanSocketDisconnectListener socketDisconnectListener,
              Socket socket){
        mHandlerMgr = handlerManager;
        mSocketListener = socketListener;
        mDisconnectListener = socketDisconnectListener;
        mSocket = socket;
    }

    public void init(){
        //read thread
        mReadObservale = Observable.create(new SocketReadObservable(mSocket));
        mReadObservale.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Packet>(){

                    @Override
                    public void onSubscribe(Disposable d) {
                        mSocketListener.onReadStart(getIP(), mName);
                    }

                    @Override
                    public void onNext(Packet packet) {
                        com.astatus.easysocketlan.PacketHandler handler = mHandlerMgr.getHandler(packet.getCode());
                        if (handler != null){
                            handler.parser(packet);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mSocketListener.onReadError(getIP(), mName, e.getMessage());

                        mDisconnectListener.onDisconnect(getIP(), mName);

                    }

                    @Override
                    public void onComplete() {

                    }
                });


        //write thread
        mWriteObservale = Observable.create(new SocketWriteObservable(mSocket, mWaitPackets));
        mWriteObservale.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>(){

                    @Override
                    public void onSubscribe(Disposable d) {
                        mSocketListener.onWriteStart(getIP(), mName);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        mSocketListener.onWrite(getIP(), mName);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mSocketListener.onWriteError(getIP(), mName, e.getMessage());
                        mDisconnectListener.onDisconnect(getIP(), mName);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    public String getIP(){
        return mSocket.getInetAddress().toString();
    }

    public void destroy(){
        if (mReadObservale != null){
            mReadObservale.unsubscribeOn(Schedulers.io());
            mReadObservale = null;
        }

        if (mWriteObservale != null){
            mWriteObservale.unsubscribeOn(Schedulers.io());
            mWriteObservale = null;
        }

        mSocketListener = null;
        mHandlerMgr = null;
    }

    public void setName(String name){
        mName = name;
    }

    public boolean send(Packet packet){
        if (mWaitPackets.offer(packet)){
            return false;
        }

        return true;
    }
}