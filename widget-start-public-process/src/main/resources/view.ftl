<div id="PcdTriagem_${instanceId}" class="wcm-widget-class super-widget fluig-style-guide pcd-triagem"
     data-params="PcdTriagem.instance()">
  <form role="form" onsubmit="return false;" autocomplete="off">
    <h2>Triagem de Pessoa com Deficiencia (PCD)</h2>
    <p class="pcd-aviso">Saude Ocupacional. Seus dados sao sigilosos e serao vistos apenas pela equipe medica.</p>

    <div class="form-group">
      <label>Nome completo *</label>
      <input type="text" class="form-control" data-pcd="nomeCompleto" maxlength="150">
    </div>
    <div class="form-group">
      <label>Funcao</label>
      <input type="text" class="form-control" data-pcd="funcao" maxlength="100">
    </div>
    <div class="form-group">
      <label>CPF *</label>
      <input type="text" class="form-control" data-pcd="cpf" maxlength="14" placeholder="000.000.000-00">
    </div>
    <div class="form-group">
      <label>WhatsApp com DDD *</label>
      <input type="text" class="form-control" data-pcd="whatsapp" maxlength="20" placeholder="(71) 90000-0000">
    </div>
    <div class="form-group">
      <label>Tipo de deficiencia *</label>
      <select class="form-control" data-pcd="tipoDeficiencia">
        <option value="">Selecione</option>
        <option>Física</option><option>Auditiva</option><option>Visual</option>
        <option>Mental</option><option>Múltipla</option>
      </select>
    </div>
    <div class="form-group">
      <label>Origem da deficiencia *</label>
      <select class="form-control" data-pcd="origemDeficiencia">
        <option value="">Selecione</option>
        <option>Congênita</option><option>Doença</option><option>Pós-operatório</option>
        <option>Acidente comum</option><option>Acidente de trabalho</option>
      </select>
    </div>
    <div class="form-group">
      <label>Dificuldades no trabalho</label>
      <textarea class="form-control" rows="3" data-pcd="dificuldades" maxlength="2000"></textarea>
    </div>
    <div class="form-group">
      <label>Queixa de saude</label>
      <textarea class="form-control" rows="3" data-pcd="queixaSaude" maxlength="2000"></textarea>
    </div>

    <!-- campo isca anti-robo: deve ficar vazio -->
    <div class="pcd-isca" aria-hidden="true">
      <input type="text" data-pcd="website" tabindex="-1" autocomplete="off">
    </div>

    <div class="checkbox">
      <label><input type="checkbox" data-pcd-ciencia> Declaro ciencia e autorizo o uso destes dados pela Saude Ocupacional. *</label>
    </div>

    <button type="button" class="btn btn-primary" data-pcd-enviar>Enviar</button>
    <div class="pcd-msg" data-pcd-msg></div>
  </form>
</div>
