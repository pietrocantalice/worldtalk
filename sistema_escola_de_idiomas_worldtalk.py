from datetime import datetime, timedelta


class Professor:
    def __init__(self, id_prof, nome, idioma):
        self.id_prof = id_prof
        self.nome = nome
        self.idioma = idioma

    def __str__(self):
        return f"Prof. {self.nome} - {self.idioma}"


class Curso:
    NIVEIS = ["Básico", "Intermediário", "Avançado"]

    def __init__(self, codigo, idioma):
        self.codigo = codigo
        self.idioma = idioma

    def __str__(self):
        return f"{self.idioma} ({self.codigo})"


class Material:
    def __init__(self, codigo, titulo, idioma, apenas_vip=False):
        self.codigo = codigo
        self.titulo = titulo
        self.idioma = idioma
        self.apenas_vip = apenas_vip
        self.emprestado = False


class Emprestimo:
    def __init__(self, aluno, material, data_retirada):
        self.aluno = aluno
        self.material = material
        self.data_retirada = data_retirada
        self.prazo_devolucao = data_retirada + timedelta(days=7)
        self.data_devolucao = None

    def devolver(self, data_entrega):
        self.data_devolucao = data_entrega
        self.material.emprestado = False


class AulaParticular:
    def __init__(self, numero, aluno, professor, horario, valor):
        self.numero = numero
        self.aluno = aluno
        self.professor = professor
        self.horario = horario
        self.valor = valor
        self.cancelada = False
        self.multa = 0.0

    def cancelar(self, momento_cancelamento):
        if self.cancelada:
            return 0.0

        self.cancelada = True
        tempo_restante = self.horario - momento_cancelamento

        # Se faltar menos de 24h, cobra 50% de taxa.
        # Quando a aula fazia parte do pacote VIP (valor 0), usamos R$ 80 como base da multa.
        base_calculo = self.valor if self.valor > 0 else 80.0
        if tempo_restante < timedelta(hours=24):
            self.multa = base_calculo * 0.5
        return self.multa


class HistoricoNivel:
    def __init__(self, curso, nivel):
        self.curso = curso
        self.nivel = nivel
        self.notas = []
        self.total_aulas = 0
        self.presencas = 0
        self.concluido = False

    def media(self):
        if len(self.notas) == 0:
            return 0.0
        return sum(self.notas) / len(self.notas)

    def frequencia(self):
        if self.total_aulas == 0:
            return 0.0
        return (self.presencas / self.total_aulas) * 100

    def passou(self):
        self.concluido = self.media() >= 7.0 and self.frequencia() >= 75.0
        return self.concluido


class Pagamento:
    def __init__(self, numero, referencia, valor_cheio, desconto, valor_pago, data):
        self.numero = numero
        self.referencia = referencia
        self.valor_cheio = valor_cheio
        self.desconto = desconto
        self.valor_pago = valor_pago
        self.data = data

    def __str__(self):
        data_fmt = self.data.strftime("%d/%m/%Y")
        return (f"[{self.numero}] {data_fmt} - {self.referencia} | "
                f"Total: R$ {self.valor_cheio:.2f} | Desc: R$ {self.desconto:.2f} | "
                f"Pago: R$ {self.valor_pago:.2f}")


class Aluno:
    def __init__(self, matricula, nome, vip=False):
        self.matricula = matricula
        self.nome = nome
        self.vip = vip
        self.cursos_concluidos = 0
        self.boletim = []
        self.pagamentos = []
        self.emprestimos = []
        self.aulas_vip_usadas = 0

    def livros_com_aluno(self):
        ativos = []
        for emp in self.emprestimos:
            if emp.data_devolucao is None:
                ativos.append(emp)
        return ativos

    def fechar_nivel(self, registro):
        if not registro.passou():
            return (f"{self.nome} não atingiu os critérios em {registro.curso.idioma} ({registro.nivel}). "
                    f"Média: {registro.media():.1f} | Freq: {registro.frequencia():.0f}%")

        idx = Curso.NIVEIS.index(registro.nivel)
        if idx + 1 < len(Curso.NIVEIS):
            prox_nivel = Curso.NIVEIS[idx + 1]
            self.boletim.append(HistoricoNivel(registro.curso, prox_nivel))
            return f"{self.nome} avançou para {registro.curso.idioma} - {prox_nivel}."
        else:
            self.cursos_concluidos += 1
            return f"{self.nome} finalizou todo o curso de {registro.curso.idioma}!"


class Turma:
    def __init__(self, codigo, curso, nivel, modalidade, horario, professor):
        self.codigo = codigo
        self.curso = curso
        self.nivel = nivel
        self.modalidade = modalidade
        self.horario = horario
        self.professor = professor

        if modalidade == "Presencial":
            self.limite_vagas = 15
            self.mensalidade = 300.0
        else:
            self.limite_vagas = 30
            self.mensalidade = 220.0

        self.matriculados = []
        self.fila_espera = []

    def adicionar_aluno(self, aluno):
        if aluno in self.matriculados or aluno in self.fila_espera:
            return f"{aluno.nome} já está cadastrado nessa turma."

        if len(self.matriculados) < self.limite_vagas:
            self.matriculados.append(aluno)
            aluno.boletim.append(HistoricoNivel(self.curso, self.nivel))
            return f"{aluno.nome} matriculado na turma {self.codigo} ({self.modalidade})."

        # Turma cheia -> vai pra lista de espera (VIP fura a fila dos regulares)
        if aluno.vip:
            pos = 0
            for i, pessoa in enumerate(self.fila_espera):
                if pessoa.vip:
                    pos = i + 1
            self.fila_espera.insert(pos, aluno)
            return f"Turma {self.codigo} lotada. {aluno.nome} (VIP) entrou na espera na posição {pos + 1}."
        else:
            self.fila_espera.append(aluno)
            return f"Turma {self.codigo} lotada. {aluno.nome} entrou na espera na posição {len(self.fila_espera)}."

    def remover_aluno(self, aluno):
        if aluno not in self.matriculados:
            return f"{aluno.nome} não faz parte dessa turma."

        self.matriculados.remove(aluno)
        retorno = f"{aluno.nome} saiu da turma {self.codigo}."

        if len(self.fila_espera) > 0:
            proximo = self.fila_espera.pop(0)
            self.matriculados.append(proximo)
            proximo.boletim.append(HistoricoNivel(self.curso, self.nivel))
            retorno += f" Vaga repassada para {proximo.nome} ({'VIP' if proximo.vip else 'Regular'})."

        return retorno


class WorldTalk:
    def __init__(self):
        self.cursos = []
        self.professores = []
        self.alunos = []
        self.turmas = []
        self.acervo = []
        self.aulas_particulares = []
        self.contador_pagamentos = 1
        self.contador_aulas = 1

    def cadastrar_curso(self, codigo, idioma):
        c = Curso(codigo, idioma)
        self.cursos.append(c)
        return c

    def cadastrar_professor(self, id_prof, nome, idioma):
        p = Professor(id_prof, nome, idioma)
        self.professores.append(p)
        return p

    def cadastrar_aluno(self, matricula, nome, vip=False):
        a = Aluno(matricula, nome, vip)
        self.alunos.append(a)
        return a

    def abrir_turma(self, codigo, curso, nivel, modalidade, horario, professor):
        t = Turma(codigo, curso, nivel, modalidade, horario, professor)
        self.turmas.append(t)
        return t

    def adicionar_material(self, codigo, titulo, idioma, apenas_vip=False):
        m = Material(codigo, titulo, idioma, apenas_vip)
        self.acervo.append(m)
        return m

    def marcar_aula_particular(self, aluno, professor, data_hora):
        if not aluno.vip:
            raise Exception("Somente alunos VIP podem agendar aulas particulares.")

        # 2 aulas inclusas por mês no plano VIP, acima disso R$ 80 cada
        valor = 0.0 if aluno.aulas_vip_usadas < 2 else 80.0

        aula = AulaParticular(self.contador_aulas, aluno, professor, data_hora, valor)
        self.contador_aulas += 1
        aluno.aulas_vip_usadas += 1
        self.aulas_particulares.append(aula)
        return aula

    def emprestar_livro(self, aluno, material, data_hoje):
        if material.emprestado:
            raise Exception("Esse material já está emprestado no momento.")
        if material.apenas_vip and not aluno.vip:
            raise Exception("Material exclusivo para assinantes VIP.")

        limite = 4 if aluno.vip else 2
        if len(aluno.livros_com_aluno()) >= limite:
            raise Exception(f"Limite de {limite} empréstimos simultâneos atingido.")

        material.emprestado = True
        emp = Emprestimo(aluno, material, data_hoje)
        aluno.emprestimos.append(emp)
        return emp

    def cobrar_mensalidade(self, aluno, turma, data_pagamento):
        valor_bruto = turma.mensalidade + (150.0 if aluno.vip else 0.0)
        desconto = valor_bruto * 0.10 if aluno.cursos_concluidos >= 2 else 0.0
        valor_liquido = valor_bruto - desconto

        desc_texto = f"{turma.curso.idioma} ({turma.modalidade})"
        if aluno.vip:
            desc_texto += " + VIP"

        pag = Pagamento(self.contador_pagamentos, desc_texto, valor_bruto, desconto, valor_liquido, data_pagamento)
        self.contador_pagamentos += 1
        aluno.pagamentos.append(pag)
        return pag


# Testes práticos do sistema
if __name__ == "__main__":
    escola = WorldTalk()
    hoje = datetime(2026, 11, 22, 14, 0)

    ing = escola.cadastrar_curso("ING01", "Inglês")
    esp = escola.cadastrar_curso("ESP01", "Espanhol")

    prof1 = escola.cadastrar_professor(1, "Ana Clara", "Inglês")
    prof2 = escola.cadastrar_professor(2, "Carlos Mendez", "Espanhol")

    lucas = escola.cadastrar_aluno(101, "Lucas Mendes", vip=False)
    lucas.cursos_concluidos = 2  # já terminou 2 cursos, tem direito a 10% de desconto
    mariana = escola.cadastrar_aluno(102, "Mariana Costa", vip=True)
    rafael = escola.cadastrar_aluno(103, "Rafael Torres", vip=True)

    print("--- TESTE 1: MATRÍCULAS E FILA DE ESPERA ---")
    turma_ing = escola.abrir_turma("T-ING-B1", ing, "Básico", "Presencial", "Seg/Qua 19h", prof1)
    print(turma_ing.adicionar_aluno(lucas))
    print(turma_ing.adicionar_aluno(mariana))

    # Preenchendo as outras 13 vagas da turma presencial
    for n in range(13):
        temp = escola.cadastrar_aluno(200 + n, f"Estudante {n+1}")
        turma_ing.adicionar_aluno(temp)

    bruno = escola.cadastrar_aluno(104, "Bruno Silva", vip=False)
    print(turma_ing.adicionar_aluno(bruno))
    print(turma_ing.adicionar_aluno(rafael))  # VIP entra na frente do Bruno
    print("Fila atual:", [f"{a.nome} ({'VIP' if a.vip else 'Comum'})" for a in turma_ing.fila_espera])
    print(turma_ing.remover_aluno(lucas))

    print("\n--- TESTE 2: APROVAÇÃO E AVANÇO DE NÍVEL ---")
    hist = mariana.boletim[0]
    hist.notas = [8.5, 9.0, 7.5]
    hist.total_aulas = 20
    hist.presencas = 18
    print(mariana.fechar_nivel(hist))

    print("\n--- TESTE 3: AULAS PARTICULARES ---")
    a1 = escola.marcar_aula_particular(mariana, prof1, hoje + timedelta(days=2))
    a2 = escola.marcar_aula_particular(mariana, prof1, hoje + timedelta(days=4))
    a3 = escola.marcar_aula_particular(mariana, prof1, hoje + timedelta(hours=8))
    print(f"Valor 1ª aula: R$ {a1.valor:.2f} | Valor 3ª aula (extra): R$ {a3.valor:.2f}")
    multa = a3.cancelar(hoje)
    print(f"Multa por cancelar a 3ª aula em cima da hora: R$ {multa:.2f}")

    print("\n--- TESTE 4: BIBLIOTECA VIRTUAL ---")
    livro_vip = escola.adicionar_material(10, "Conversação Avançada", "Inglês", apenas_vip=True)
    emp = escola.emprestar_livro(mariana, livro_vip, hoje)
    print(f"Empréstimo: {emp.material.titulo} | Devolver até {emp.prazo_devolucao.strftime('%d/%m/%Y')}")
    emp.devolver(hoje + timedelta(days=3))
    print("Livro devolvido. Ainda está emprestado?", livro_vip.emprestado)

    print("\n--- TESTE 5: MENSALIDADES ---")
    print(escola.cobrar_mensalidade(lucas, turma_ing, hoje))
    print(escola.cobrar_mensalidade(mariana, turma_ing, hoje))