package br.com.paulork.mailutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EMail implements Cloneable {

    private String server;
    private Integer port;
    private String user;
    private String pass;
    private AuthType authType;

    private String remetente;
    private String mensagem;
    private String assunto;
    private List<String> para = new ArrayList<>();
    private Map<String, byte[]> anexos = new HashMap<>();
    private String contentType = "text/html; charset=UTF-8";
    private boolean confirmLeitura;
    private boolean confirmEntrega;

    public EMail(String server, Integer port, String user, String pass, Boolean tls, Boolean ssl) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.pass = pass;
        remetente = user;
        if (tls) {
            authType = AuthType.TLS;
        }
        if (ssl) {
            authType = AuthType.SSL;
        }
    }

    public void setContentType(String tipo) {
        contentType = tipo;
    }

    public void addAnexo(String nomeArquivo, byte[] arquivo) {
        anexos.put(nomeArquivo, arquivo);
    }

    public void addDestinatario(String email) {
        para.add(email);
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public void setAssunto(String assunto) {
        this.assunto = assunto;
    }

    public void setPara(List<String> para) {
        this.para = para;
    }

    public void setAnexos(Map<String, byte[]> anexos) {
        this.anexos = anexos;
    }

    public void setRemetente(String remetente) {
        this.remetente = remetente;
    }

    public void setConfirmLeitura(boolean confirmLeitura) {
        this.confirmLeitura = confirmLeitura;
    }

    public void setConfirmEntrega(boolean confirmEntrega) {
        this.confirmEntrega = confirmEntrega;
    }

    public String getServer() {
        return server;
    }

    public Integer getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public String getRemetente() {
        return remetente;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getAssunto() {
        return assunto;
    }

    public List<String> getPara() {
        return para;
    }

    public Map<String, byte[]> getAnexos() {
        return anexos;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isConfirmLeitura() {
        return confirmLeitura;
    }

    public boolean isConfirmEntrega() {
        return confirmEntrega;
    }

    @Override
    protected EMail clone() throws CloneNotSupportedException {
        EMail clone = (EMail) super.clone();
        return clone;
    }

    public void enviar() {
        try {
            EMailPool.getInstance().addEMail(this.clone());
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Não foi possível adicionar o EMail ao pool!", ex);
        }
    }
}
