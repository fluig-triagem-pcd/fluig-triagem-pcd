var PcdTriagem = SuperWidget.extend({
    enviando: false,

    init: function () {
        var that = this;
        var $raiz = this.DOM;
        $raiz.find('[data-pcd-enviar]').on('click', function () { that.enviar(); });
    },

    campo: function (nome) {
        return $.trim(this.DOM.find('[data-pcd="' + nome + '"]').val() || '');
    },

    mensagem: function (texto, ok) {
        this.DOM.find('[data-pcd-msg]').text(texto).removeClass('ok erro').addClass(ok ? 'ok' : 'erro');
    },

    enviar: function () {
        var that = this;
        if (this.enviando) { return; }

        var ciencia = this.DOM.find('[data-pcd-ciencia]').is(':checked') ? 'Sim' : 'Não';
        var dados = {
            nomeCompleto: this.campo('nomeCompleto'),
            funcao: this.campo('funcao'),
            cpf: this.campo('cpf'),
            whatsapp: this.campo('whatsapp'),
            tipoDeficiencia: this.campo('tipoDeficiencia'),
            origemDeficiencia: this.campo('origemDeficiencia'),
            dificuldades: this.campo('dificuldades'),
            queixaSaude: this.campo('queixaSaude'),
            website: this.campo('website'),
            ciencia: ciencia
        };

        if (dados.nomeCompleto.length < 3) { return this.mensagem('Informe o nome completo.', false); }
        if (dados.cpf.replace(/\D/g, '').length !== 11) { return this.mensagem('Informe um CPF válido.', false); }
        if (dados.whatsapp.replace(/\D/g, '').length < 10) { return this.mensagem('Informe o WhatsApp com DDD.', false); }
        if (!dados.tipoDeficiencia) { return this.mensagem('Selecione o tipo de deficiência.', false); }
        if (!dados.origemDeficiencia) { return this.mensagem('Selecione a origem da deficiência.', false); }
        if (ciencia !== 'Sim') { return this.mensagem('É necessário declarar ciência e autorizar.', false); }

        this.enviando = true;
        this.mensagem('Enviando...', true);

        $.ajax({
            url: WCMAPI.serverURL + '/start-public-process-api/api/v1/pcd/enviar',
            type: 'POST',
            contentType: 'application/json; charset=UTF-8',
            data: JSON.stringify(dados),
            dataType: 'json'
        }).done(function (r) {
            that.mensagem((r && r.mensagem ? r.mensagem : 'Enviado com sucesso.') +
                          (r && r.protocolo ? ' Protocolo: ' + r.protocolo : ''), true);
            that.DOM.find('input[type=text], textarea').val('');
            that.DOM.find('select').val('');
            that.DOM.find('[data-pcd-ciencia]').prop('checked', false);
        }).fail(function (xhr) {
            var msg = 'Não foi possível enviar agora. Tente novamente mais tarde.';
            try { msg = JSON.parse(xhr.responseText).mensagem || msg; } catch (e) {}
            that.mensagem(msg, false);
        }).always(function () {
            that.enviando = false;
        });
    }
});
