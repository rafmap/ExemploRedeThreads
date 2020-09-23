package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    String ipDevice;
    TextView tv;
    ImageView iv;
    EditText edt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.textView);
        iv = (ImageView) findViewById(R.id.imageView);
        edt = (EditText) findViewById(R.id.edtCEP);
        NotificacaoConexao nc = new NotificacaoConexao();
    }

    public void onclickListar(View v) {
        listConnections(); //lista todas as conexões
        if (estaWifiConectado()) {
            // Se o Wi-Fi estiver conectado, tentarei ligar o servidor
            tv.setText("Wi-Fi conectado em: " + ipDevice);
            //baixarImagem(); //Erro porque usa rede na Thread Main (código bloqueado porque dura mais de 5 s - "código blockante")
            //baixarImagemThreads(); //Baixa a Imagem com Threads, conta os bytes, mas não faz nada
            //baixarImagemEColocarNaView();//Erro ao acessar a View por outra Thread
            //baixarImagemEColocarNaViewII();//Finalmente baixa e mostra a imagem sem problemas
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    //executarACalculadora();
                    transformarCEP();
                }
            });
            t.start();

        } else {
            String texto = "Mano, liga o Wi-Fi";
            int duracao = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, texto, duracao);
            toast.show();
        }

    }

    private void transformarCEP() {
        //https://viacep.com.br/ws/60115222/json/
        String CEP = edt.getText().toString();
        //"60115222";

        try {
            URL url = new URL("https://viacep.com.br/ws/" + CEP + "/json/");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();//abertura da conexão TCP
            conn.setReadTimeout(10000);//timeout da conexão
            conn.setConnectTimeout(15000);//para ficar esperando
            conn.setRequestMethod("GET");//serviço esperando uma conexão do tipo GET
            conn.setDoInput(true);//Vou Ler Dados? - Pegar a resposta da calculadora (GET)
            conn.connect();//método que vai realizar a conexão enviando o método GET

            //RECEPÇÃO
            String result[] = new String[1];
            int responseCode = conn.getResponseCode();//vai receber a resposta dessaconexão
            //nesse momento vai ficar bloqueado esperando o servidor mandar as respostas

            if (responseCode == HttpsURLConnection.HTTP_OK) {//só pegará erro se o código retornado for 200
                //se o erro for 404, ele nem será pego
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }//montada estrutura que concatena a resposta e coloca dentro da variável result[0]
                result[0] = response.toString();
                Log.v("PDM", "Resultado: " + result[0]);
                //Acessou um serviço REST e recebeu um JSON como resposta

                //Classe do pacote org.json do Android
                JSONObject respostaJSON = new JSONObject(result[0]);
                //através dela podem ser lidos os atributos do JSON e seus valores
                final String loc = respostaJSON.getString("logradouro");
                String cidade = respostaJSON.getString("localidade");

                Log.v("PDM", "Esse é o CEP da " + loc + " da cidade " + cidade);

                tv.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(loc);
                    }
                });

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executarACalculadora() {
        //https://double-nirvana-273602.appspot.com/?hl=pt-BR
        int oper1, oper2, operacao;
        oper1 = 14;
        oper2 = 22;
        operacao = 1;

        try {
            URL url = new URL("https://double-nirvana-273602.appspot.com/?hl=pt-BR");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();//abertura da conexão TCP
            conn.setReadTimeout(10000);//timeout da conexão
            conn.setConnectTimeout(15000);//para ficar esperando
            conn.setRequestMethod("POST");//serviço esperando uma conexão do tipo POST
            conn.setDoInput(true);//Vou Ler Dados? - Pegar a resposta da calculadora (seria um GET)
            conn.setDoOutput(true);//Vou Enviar Dados? - Vai enviar os parâmetros via POST - é false por padrão se não for colocado

            //ENVIO
            //Dados via POST são enviados via o Body da conexão - dados do streaming de conexão
            //Criar objetos do Java para lidar com comunicação de streaming
            OutputStream os = conn.getOutputStream();//vai pegar a conexão conn e pegar o streaming de saída dela
            //agora tudo que for colocado no OutputStream, vai como Body dos dados que estão sendo enviados

            //tudo que for escrito no writer está indo para a conexão de saída dessa conexão HTTPS
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            //precisa separar os parâmetros usando "&" quando envia dados via POST ou recebe dados via GET
            writer.write("oper1=" + oper1 + "&oper2=" + oper2 + "&operacao=" + operacao);
            writer.flush();//enviar os dados
            writer.close();//sempre fechar o BufferedWriter
            os.close();//sempre fechar o OutputStream

            //RECEPÇÃO
            String result[] = new String[1];
            int responseCode = conn.getResponseCode();//vai receber a resposta dessaconexão
            //nesse momento vai ficar bloqueado esperando o servidor mandar as respostas

            if (responseCode == HttpsURLConnection.HTTP_OK) {//se deu tudo certo, pode-se ler a resposta
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }//montada estrutura que concatena a resposta e coloca dentro da variável result[0]
                result[0] = response.toString();
                Log.v("PDM", "Resultado: " + result[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void baixarImagemEColocarNaView() {
        Log.v("PDM", "BAIXANDO IMAGEM E COLANDO NA VIEW");
        Bitmap b;
        //Thread Created
        Thread t = new Thread(new Runnable() {//criando uma classe Runnable
            @Override
            public void run() {
                //não se pode rodar este código na thread principal
                //mas uma vez colocado em outra thread, não se pode passar a informação para a thread principal
                Bitmap b = loadImageFromNetwork("http://pudim.com.br/pudim.jpg");//baixa a imagem
                //imprime a quantidade de bytes da imagem
                Log.v("PDM", "Imagem baixada com " + b.getByteCount() + " bytes");
                try {
                    iv.setImageBitmap(b);
                } catch (Exception e) {
                    Log.v("PDM", "Não é possível acessar a Thread UI");
                    e.printStackTrace();
                }
            }
        }
        );
        //só aqui a thread vira Alive
        t.start();//"oi, máquina virtual, tô aqui, este é meu pedaço de código. Quando puder, dê o run"
        //não vai executar imediatamente depois, vai depender do escalonador de threads do SO,
        //que vai ter um conjunto de threads para executar e vai decidir o que fazer,
        //inclusive pode ficar alternando dentro do código (inter-living - concorrência)
    }


    private void baixarImagemEColocarNaViewII() {
        Log.v("PDM", "BAIXANDO IMAGEM E COLANDO NA VIEW");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //final para ser acessível de inner class
                final Bitmap b = loadImageFromNetwork("http://pudim.com.br/pudim.jpg");
                Log.v("PDM", "Imagem baixada com " + b.getByteCount() + " bytes");
                try {
                    //qualquer objeto que herde de View pode chamar esse método post
                    //post é do elemento View (neste caso, ImageView)
                    //coloca-se dentro desse método um código do tipo Runnable, ou seja, que pode ser executado
                    //Runnable = código executável
                    //este código não é imediatamente executado, só vai ser executado quando for a vez dele
                    //na fila de execuções de códigos da thread principal
                    //desvantagem: código bagunçado, muito acoplado, viola o encapsulamento entre as camadas e sem pull de conexões
                    //solução: usar alguma estratégia de callbacks
                    iv.post(new Runnable() {
                        @Override
                        public void run() {
                            iv.setImageBitmap(b);//altera a imagem do ImageView
                        }
                    });

                } catch (Exception e) {
                    Log.v("PDM", "Não é possível acessar a Thread UI");
                    e.printStackTrace();
                }
            }
        }
        );
        t.start();

    }

    public void onClickTravar(View v) {
        Log.v("PDM", "Vou tentar travar");
        for (long i = 0; i < 10000000; i++) {
            for (long j = 0; j < 1000000000; j++) {

            }
        }
        Log.v("PDM", "Terminou o loop?");
    }

    public void baixarImagem() {
        Log.v("PDM", "BAIXANDO IMAGEM SEM THREAD");
        Bitmap b = loadImageFromNetwork("http://pudim.com.br/pudim.jpg");
        //mImageView.setImageBitmap(b);

    }


    public void baixarImagemThreads() {
        Log.v("PDM", "BAIXANDO IMAGEM COM THREAD");
        Bitmap b;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                //coloca a imagem num componente visual
                //Android não deixa a thread criada chamar um componente visual
                Bitmap b = loadImageFromNetwork("http://pudim.com.br/pudim.jpg");
                Log.v("PDM", "Imagem baixada com " + b.getByteCount() + " bytes");
            }
        }
        );
        t.start();


    }

    //código que baixa a imagem
    private Bitmap loadImageFromNetwork(String url) {
        try {
            //classe BitmapFactory a que você passa uma URL, e ela baixa para você uma imagem
            Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(url).getContent());
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean estaWifiConectado() {
        ConnectivityManager connManager;//classe para pegar informações de conectividade
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);//retorna um Connectivity Manager
        Network[] networks = connManager.getAllNetworks();//
        NetworkInfo networkInfo;//classe para pegar informações da rede

        boolean temConexao = false;

        for (Network mNetwork : networks) {//percorrer esse vetor
            networkInfo = connManager.getNetworkInfo(mNetwork);
            //Método depreciado, na versão mais nova se sugere usar callback para isso, mas por enquanto é suficiente
            //vai entrar neste if quando uma conexão está conectada
            if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {

                NetworkCapabilities networkFeatures;//maneira mais correta de pegar informações sobre a capacidade da rede
                //a partir de uma rede consegue-se saber essa informação
                networkFeatures = connManager.getNetworkCapabilities(mNetwork);
                //só vai entrar neste if somente se esta rede conectada tem capacidade de Wi-Fi
                if (networkFeatures.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    temConexao = true;

                    Log.v("PDM", "Tem acesso a WI-FI");
                    //Com esta classe pode-se pegar informações como MAC Address, IP Address para, por exemplo,
                    //montar um servidor e passar essas informações para um cliente
                    //Permite fazer perguntas sobre o Wi-Fi
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    //Pode-se usar este tipo de informação para autenticar seu aparelho, saber que é um aparelho único
                    //já que o MAC Address é único para cada um dos dispositivos
                    String macAddress = wifiManager.getConnectionInfo().getMacAddress();
                    //Pode-se pegar o IP que estou conectado
                    //IP é inteiro
                    int ip = wifiManager.getConnectionInfo().getIpAddress();
                    //Conjunto de operações que transforma o IP de inteiro para String
                    //Sequência de bytes (4 inteiros - %d.%d.%d.%d)
                    //Operações de deslocamento para pegar só a parte do número que forma o 1º byte do endereço (ip & 0xff),
                    //o 2º byte (ip >> 8 & 0xff) (deslocamento de 1 byte)
                    // o 3º byte (ip >> 16 & 0xff) (deslocamento de 2 bytes)
                    // e o último byte do endereço IP (deslocamento de 3 bytes)
                    String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
                    Log.v("PDM", "IP:" + ipAddress);
                    Log.v("PDM", "MAC:" + macAddress);
                    //emulador funciona como um novo dispositivo na rede, ou seja, esse IP do emulador é alcançável dentro
                    //da rede Wi-Fi em que se está
                    ipDevice = ipAddress;

                    //Vamos ouvir mudanças nessa conexão
                    NetworkRequest.Builder builder = new NetworkRequest.Builder();
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                    NotificacaoConexao nfc = new NotificacaoConexao();
                    connManager.registerNetworkCallback(builder.build(), nfc);

                }

            }
        }
        return temConexao;

    }

    public void listConnections() {
        ConnectivityManager connManager;
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connManager.getAllNetworks();
        NetworkInfo networkInfo;


        for (Network mNetwork : networks) {
            networkInfo = connManager.getNetworkInfo(mNetwork);
            if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {

                Log.v("PDM", "tipo de conexão conectada:" + networkInfo.getTypeName());

            }
        }

    }

    //soccets
    private void ligarServidorSocket() {
        //para criar um servidor socket, precisa-se de um ServerSocket
        ServerSocket welcomeSocket;//estabelece o three-way handshake
        try {
            //cria um socket numa determinada porta e vai ficar bloqueado esperando uma conexão
            welcomeSocket = new ServerSocket(9090);//cria um socket numa determinada porta
            Socket connectionSocket = welcomeSocket.accept();//fica esperando a conexão para fazer a comunicação

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

class NotificacaoConexao extends ConnectivityManager.NetworkCallback {

    //Um Call Back para saber se houve ou não mudanças no estado da conexão
    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);
        Log.v("PDM", "Wi-Fi está conectado");
    }

    @Override
    public void onLost(Network network) {
        super.onLost(network);
        Log.i("PDM", "Desligaram o Wi-Fi");
    }
}