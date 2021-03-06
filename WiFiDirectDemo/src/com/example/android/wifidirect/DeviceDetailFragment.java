/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
//THISIS ConnectionInfoListener
/** Interface for callback invocation when connection info is available */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    private ArrayList<String> chatStringList = new ArrayList<String>();
    private ListView chatListView;
    private ArrayAdapter<String> chatListAdapater;
   
    private TextView statusText;
    private LinearLayout chatTypingLayout;
    private EditText chatEdtText;
    private Button chatSendButton;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);

        //add chatting
        chatListView = (ListView)mContentView.findViewById(R.id.chat_list);
        chatListAdapater = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
        		chatStringList);
        chatListView.setAdapter(chatListAdapater);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        
        chatStringList.add("Test string");
        chatListAdapater.notifyDataSetChanged();
        
        //chat typing
        chatTypingLayout = (LinearLayout)mContentView.findViewById(R.id.chat_typing);
        chatTypingLayout.setVisibility(View.GONE);
        chatEdtText = (EditText)mContentView.findViewById(R.id.chat_edttext);
        chatSendButton = (Button)mContentView.findViewById(R.id.chat_btn);
        statusText = (TextView)mContentView.findViewById(R.id.status_text);
        
        //original
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
                        );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        //intent.setType("image/*");
                        //startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    	//client가 파일 보낼때
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }*/

    //implement ConnectionInfoListener
    //peer간 연결되면 호출되는 리스너
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
    	Log.d(WiFiDirectActivity.TAG, "onConnectionInfoAvailable called");
    	statusText.setText("onConnectionInfoAvailable called");
    	
    	if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        //THISIS IP주소 등을 받아와서 infoView에 뿌림
        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        //THISIS 파일 전송 태스크 오픈
        if (info.groupFormed && info.isGroupOwner) {	/** groupFormed -> Indicates if a p2p group has been successfully formed */
            /*new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
                    .execute();*/
        	
        	chatTypingLayout.setVisibility(View.VISIBLE);
        	
        	//server
        	statusText.setText("server: open chat server task");
        	Log.d("Socket", "server: open chat server task");
        	//new ChatServerOpenTask(getActivity(), chatSendButton, chatEdtText, statusText).execute();
        	socketHandler.sendMessage(Message.obtain(socketHandler, 1));
        	
        	
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            /*mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));*/
        	
        	chatTypingLayout.setVisibility(View.GONE);
        	
        	//client
        	//pc devices 랑연결되면 client 임 -> 소켓열어서 텍스트 받기 테스트
        	socketHandler.sendMessage(Message.obtain(socketHandler, 0, info));
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }
    
    private Handler socketHandler = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		if(msg.what == 0) {
	    		try {
	    			final WifiP2pInfo info = (WifiP2pInfo)msg.obj;
	    				        		
	        		Thread readThread = new Thread() {
	        			@Override
	        			public void run() {
	        				Log.d("Socket Thread", "Client Thread Start");
	        				try {
	        					
	        					setStatusString("client: request connection");
		    	            	
		    	            	final Socket socket = new Socket();
		    	            	
		    	            	socket.bind(null);
		    	            	setStatusString("client: binding done, connet to + " + info.groupOwnerAddress.getHostAddress());
		    	            	
		    	            	while(true) {
		    	            		setStatusString("connectiong to ..... " + info.groupOwnerAddress.getHostAddress());
		    	            		try {
		    	            			socket.connect(new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), 9190), 5000);
		    	            			break;
		    	            		} catch(ConnectException ce) {
		    	            			ce.printStackTrace();
		    	            			setStatusString("connectiong to fail, retry");
		    	            		} catch(SocketTimeoutException te) {
		    	            			te.printStackTrace();
		    	            			setStatusString("connectiong to fail, retry");
		    	            		} catch(Exception e) {
		    	            			e.printStackTrace();
		    	            			setStatusString("connectiong to fail, retry");
		    	            		}
		    	            		Thread.sleep(1000);
		    	            	}
		    	        		//final Socket socket = new Socket(info.groupOwnerAddress.getHostAddress(), 9190);
		    	        		Log.d(WiFiDirectActivity.TAG, "client: connect socket");
		    	        		setStatusString("client: connection soceket");

		    	        		BufferedReader reader;
		    	        		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        						
		    	        		
		        				while(true) {
		        					try {
		        						String readStr = reader.readLine();		        						
		        						uiHandler.sendMessage(Message.obtain(uiHandler, 2, readStr));
		        						Thread.sleep(100);
		        						
		        					} catch(Exception e) {
		        						e.printStackTrace();
		        					}
		        				}
	        				} catch(Exception e) {
	        					e.printStackTrace();
	        				}
	        				
	        			}
	        		};
	        		readThread.start();
	        		
	        	} catch (Exception e) {
	        		e.printStackTrace();
	        	}
    		} else if(msg.what == 1) {
    			Thread serverThread = new Thread() {
    				@Override
    				public void run() {
    					Log.d("Socket Thread", "Server Thread Start");
    					try {
		                    ServerSocket serverSocket = new ServerSocket(9190);
		                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
		                    setStatusString("server: serversocket opened");
		                    
		                    final Socket client = serverSocket.accept();
		                    setStatusString("server: connection accept");
		                    Log.d(WiFiDirectActivity.TAG, "Server: connection done");
		                    
		                    PrintWriter pw = null;
    						try {
    							pw = new PrintWriter(client.getOutputStream(), true);
    						} catch (IOException e) {
    							e.printStackTrace();
    							setStatusString("print writer error!");
    							return;
    						}
		                    
    						uiHandler.sendMessage(Message.obtain(uiHandler, 1, pw));
		                    
		                    
		                } catch (IOException e) {
		                	e.printStackTrace();
		                }
    				}
    			};
    			serverThread.start();
    		} else if(msg.what == 2) {
    			chatEdtText.setText("");
    		}
    	};
    };
    
    private Handler statusTextHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		String str = (String)msg.obj;
    		statusText.setText(str);
        	Log.d("Socket", str);
    	}
    };
    
    private Handler uiHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		if(msg.what == 0) {
    			chatEdtText.setText("");
    		} else if (msg.what == 1) {
    			setStatusString("setOnSendButtonClickListener");
    			final PrintWriter pw = (PrintWriter)msg.obj;
    			
    			chatSendButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						String msg = chatEdtText.getEditableText().toString();
						try {
							pw.println(msg);
							pw.flush();
							setStatusString("server: send msg " + msg);
							Log.d(WiFiDirectActivity.TAG, "Server: send msg" + msg);
							chatEdtText.setText("");
							
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
    			});
    		} else if (msg.what == 2) {
    			String readStr = (String)msg.obj;
    			
				chatStringList.add(readStr);
		        chatListAdapater.notifyDataSetChanged();
		        
		        setStatusString("recv msg : " + readStr);
    		}
    	}
    };
    
    private void setStatusString(String statusString) {
    	statusTextHandler.sendMessage(Message.obtain(statusTextHandler, 0, statusString));
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        
        chatTypingLayout.setVisibility(View.GONE);
        
        this.getView().setVisibility(View.GONE);
    }
    
    /*
    public static class ChatServerOpenTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private Button sendBtn;
        private EditText edtChat;
        private TextView status;

        public ChatServerOpenTask(Context context, Button sendBtn, EditText edtChat, TextView statusText) {
            this.context = context;
            this.sendBtn = sendBtn;
            this.edtChat = edtChat;
            this.status = statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
        	try {
                ServerSocket serverSocket = new ServerSocket(9190);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                status.setText("server: serversocket opened");
            	Log.d("Socket", "server: serversocket opened");
                
                final Socket client = serverSocket.accept();
                status.setText("server: connection accept");
            	Log.d("Socket", "server: connection accept");
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                
                
                sendBtn.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						String msg = edtChat.getEditableText().toString();
						
						PrintWriter pw;
						try {
							pw = new PrintWriter(client.getOutputStream(), true);
							pw.println(msg);
							pw.flush();
							status.setText("server: send msg " + msg);
			            	Log.d("Socket", "sned msg" + msg);
							Log.d(WiFiDirectActivity.TAG, "Server: send msg" + msg);
							sendBtn.setText("");
							
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				});
                
                
            } catch (IOException e) {
            	e.printStackTrace();
            }
        	return null;
        }
        
        @Override
        protected void onPostExecute(String result) {
           

        }
        @Override
        protected void onPreExecute() {
        	
        }

    }*/
    
    
    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    //THISIS 그룹오너 -> open 서버소켓
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

}
