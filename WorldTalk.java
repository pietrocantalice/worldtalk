import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class Professor {
    final int idProf;
    final String nome;
    final String idioma;

    Professor(int idProf, String nome, String idioma) {
        this.idProf = idProf;
        this.nome = nome;
        this.idioma = idioma;
    }

    @Override
    public String toString() {
        return "Prof. " + nome + " - " + idioma;
    }
}

class Curso {
    static final List<String> NIVEIS = Arrays.asList(
        "Básico", "Intermediário", "Avançado");
    final String codigo;
    final String idioma;

    Curso(String codigo, String idioma) {
        this.codigo = codigo;
        this.idioma = idioma;
    }

    @Override
    public String toString() {
        return idioma + " (" + codigo + ")";
    }
}

class Material {
    final int codigo;
    final String titulo;
    final String idioma;
    final boolean apenasVip;
    boolean emprestado = false;

    Material(int codigo, String titulo, String idioma) {
        this(codigo, titulo, idioma, false);
    }

    Material(int codigo, String titulo, String idioma,
             boolean apenasVip) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.idioma = idioma;
        this.apenasVip = apenasVip;
    }
}

class Emprestimo {
    final Aluno aluno;
    final Material material;
    final LocalDateTime dataRetirada;
    final LocalDateTime prazoDevolucao;
    LocalDateTime dataDevolucao = null;

    Emprestimo(Aluno aluno, Material material,
               LocalDateTime dataRetirada) {
        this.aluno = aluno;
        this.material = material;
        this.dataRetirada = dataRetirada;
        this.prazoDevolucao = dataRetirada.plusDays(7);
    }

    void devolver(LocalDateTime dataEntrega) {
        // Evita que uma devolução repetida altere outro empréstimo.
        if (dataDevolucao != null) {
            return;
        }
        if (dataEntrega.isBefore(dataRetirada)) {
            throw new IllegalArgumentException(
                "A devolução não pode ocorrer antes da retirada.");
        }
        dataDevolucao = dataEntrega;
        material.emprestado = false;
    }
}

class AulaParticular {
    final int numero;
    final Aluno aluno;
    final Professor professor;
    final LocalDateTime horario;
    final double valor;
    boolean cancelada = false;
    double multa = 0.0;

    AulaParticular(int numero, Aluno aluno, Professor professor,
                   LocalDateTime horario, double valor) {
        this.numero = numero;
        this.aluno = aluno;
        this.professor = professor;
        this.horario = horario;
        this.valor = valor;
    }

    double cancelar(LocalDateTime momentoCancelamento) {
        if (cancelada) {
            return 0.0;
        }
        Duration tempoRestante = Duration.between(
            momentoCancelamento, horario);
        if (tempoRestante.isNegative()) {
            throw new IllegalArgumentException(
                "Não é possível cancelar após o horário da aula.");
        }
        cancelada = true;
        double baseCalculo = valor > 0 ? valor : 80.0;
        if (tempoRestante.compareTo(Duration.ofHours(24)) < 0) {
            multa = baseCalculo * 0.5;
        }
        return multa;
    }
}

class HistoricoNivel {
    final Curso curso;
    final String nivel;
    final List<Double> notas = new ArrayList<>();
    int totalAulas = 0;
    int presencas = 0;
    boolean concluido = false;
    boolean fechamentoProcessado = false;

    HistoricoNivel(Curso curso, String nivel) {
        if (!Curso.NIVEIS.contains(nivel)) {
            throw new IllegalArgumentException("Nível inválido.");
        }
        this.curso = curso;
        this.nivel = nivel;
    }

    double media() {
        if (notas.isEmpty()) {
            return 0.0;
        }
        double soma = 0.0;
        for (double nota : notas) {
            soma += nota;
        }
        return soma / notas.size();
    }

    double frequencia() {
        if (totalAulas == 0) {
            return 0.0;
        }
        return (double) presencas / totalAulas * 100;
    }

    boolean passou() {
        concluido = media() >= 7.0 && frequencia() >= 75.0;
        return concluido;
    }
}

class Pagamento {
    final int numero;
    final String referencia;
    final double valorCheio;
    final double desconto;
    final double valorPago;
    final LocalDateTime data;

    Pagamento(int numero, String referencia, double valorCheio,
              double desconto, double valorPago, LocalDateTime data) {
        this.numero = numero;
        this.referencia = referencia;
        this.valorCheio = valorCheio;
        this.desconto = desconto;
        this.valorPago = valorPago;
        this.data = data;
    }

    @Override
    public String toString() {
        String dataFmt = data.format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return String.format(Locale.forLanguageTag("pt-BR"),
            "[%d] %s - %s | Total: R$ %.2f | "
            + "Desc: R$ %.2f | Pago: R$ %.2f",
            numero, dataFmt, referencia,
            valorCheio, desconto, valorPago);
    }
}

class Aluno {
    final int matricula;
    final String nome;
    final boolean vip;
    int cursosConcluidos = 0;
    final List<HistoricoNivel> boletim = new ArrayList<>();
    final List<Pagamento> pagamentos = new ArrayList<>();
    final List<Emprestimo> emprestimos = new ArrayList<>();
    // A chave identifica o ano e o mês da aula agendada.
    final Map<YearMonth, Integer> aulasVipPorMes = new HashMap<>();

    Aluno(int matricula, String nome) {
        this(matricula, nome, false);
    }

    Aluno(int matricula, String nome, boolean vip) {
        this.matricula = matricula;
        this.nome = nome;
        this.vip = vip;
    }

    List<Emprestimo> livrosComAluno() {
        List<Emprestimo> ativos = new ArrayList<>();
        for (Emprestimo emp : emprestimos) {
            if (emp.dataDevolucao == null) {
                ativos.add(emp);
            }
        }
        return ativos;
    }

    String fecharNivel(HistoricoNivel registro) {
        if (!boletim.contains(registro)) {
            throw new IllegalArgumentException(
                "Este registro não pertence ao aluno.");
        }
        if (registro.fechamentoProcessado) {
            return "Este nível já foi encerrado para " + nome + ".";
        }
        if (!registro.passou()) {
            return String.format(Locale.forLanguageTag("pt-BR"),
                "%s não atingiu os critérios em %s (%s). "
                + "Média: %.1f | Freq: %.0f%%",
                nome, registro.curso.idioma, registro.nivel,
                registro.media(), registro.frequencia());
        }
        registro.fechamentoProcessado = true;
        int idx = Curso.NIVEIS.indexOf(registro.nivel);
        if (idx + 1 < Curso.NIVEIS.size()) {
            String proximoNivel = Curso.NIVEIS.get(idx + 1);
            boletim.add(new HistoricoNivel(
                registro.curso, proximoNivel));
            return nome + " avançou para " + registro.curso.idioma
                + " - " + proximoNivel + ".";
        }
        cursosConcluidos++;
        return nome + " finalizou todo o curso de "
            + registro.curso.idioma + "!";
    }
}

class Turma {
    final String codigo;
    final Curso curso;
    final String nivel;
    final String modalidade;
    final String horario;
    final Professor professor;
    final int limiteVagas;
    final double mensalidade;
    final List<Aluno> matriculados = new ArrayList<>();
    final List<Aluno> filaEspera = new ArrayList<>();

    Turma(String codigo, Curso curso, String nivel,
          String modalidade, String horario, Professor professor) {
        if (!Curso.NIVEIS.contains(nivel)) {
            throw new IllegalArgumentException("Nível inválido.");
        }
        if (!modalidade.equals("Presencial")
                && !modalidade.equals("Online")) {
            throw new IllegalArgumentException(
                "Modalidade deve ser Presencial ou Online.");
        }
        this.codigo = codigo;
        this.curso = curso;
        this.nivel = nivel;
        this.modalidade = modalidade;
        this.horario = horario;
        this.professor = professor;
        limiteVagas = modalidade.equals("Presencial") ? 15 : 30;
        mensalidade = modalidade.equals("Presencial") ? 300.0 : 220.0;
    }

    String adicionarAluno(Aluno aluno) {
        if (matriculados.contains(aluno) || filaEspera.contains(aluno)) {
            return aluno.nome + " já está cadastrado nessa turma.";
        }
        if (matriculados.size() < limiteVagas) {
            matriculados.add(aluno);
            aluno.boletim.add(new HistoricoNivel(curso, nivel));
            return aluno.nome + " matriculado na turma " + codigo
                + " (" + modalidade + ").";
        }
        int pos = filaEspera.size();
        if (aluno.vip) {
            pos = 0;
            for (int i = 0; i < filaEspera.size(); i++) {
                if (filaEspera.get(i).vip) {
                    pos = i + 1;
                }
            }
        }
        filaEspera.add(pos, aluno);
        return "Turma " + codigo + " lotada. " + aluno.nome
            + (aluno.vip ? " (VIP)" : "")
            + " entrou na espera na posição " + (pos + 1) + ".";
    }

    String removerAluno(Aluno aluno) {
        if (!matriculados.remove(aluno)) {
            return aluno.nome + " não faz parte dessa turma.";
        }
        String retorno = aluno.nome + " saiu da turma " + codigo + ".";
        if (!filaEspera.isEmpty()) {
            Aluno proximo = filaEspera.remove(0);
            matriculados.add(proximo);
            proximo.boletim.add(new HistoricoNivel(curso, nivel));
            retorno += " Vaga repassada para " + proximo.nome
                + " (" + (proximo.vip ? "VIP" : "Regular") + ").";
        }
        return retorno;
    }
}

public class WorldTalk {
    final List<Curso> cursos = new ArrayList<>();
    final List<Professor> professores = new ArrayList<>();
    final List<Aluno> alunos = new ArrayList<>();
    final List<Turma> turmas = new ArrayList<>();
    final List<Material> acervo = new ArrayList<>();
    final List<AulaParticular> aulasParticulares = new ArrayList<>();
    int contadorPagamentos = 1;
    int contadorAulas = 1;

    Curso cadastrarCurso(String codigo, String idioma) {
        Curso curso = new Curso(codigo, idioma);
        cursos.add(curso);
        return curso;
    }

    Professor cadastrarProfessor(int idProf, String nome, String idioma) {
        Professor professor = new Professor(idProf, nome, idioma);
        professores.add(professor);
        return professor;
    }

    Aluno cadastrarAluno(int matricula, String nome) {
        return cadastrarAluno(matricula, nome, false);
    }

    Aluno cadastrarAluno(int matricula, String nome, boolean vip) {
        Aluno aluno = new Aluno(matricula, nome, vip);
        alunos.add(aluno);
        return aluno;
    }

    Turma abrirTurma(String codigo, Curso curso, String nivel,
                     String modalidade, String horario,
                     Professor professor) {
        Turma turma = new Turma(codigo, curso, nivel,
            modalidade, horario, professor);
        turmas.add(turma);
        return turma;
    }

    Material adicionarMaterial(int codigo, String titulo, String idioma) {
        return adicionarMaterial(codigo, titulo, idioma, false);
    }

    Material adicionarMaterial(int codigo, String titulo, String idioma,
                                boolean apenasVip) {
        Material material = new Material(codigo, titulo, idioma, apenasVip);
        acervo.add(material);
        return material;
    }

    AulaParticular marcarAulaParticular(Aluno aluno, Professor professor,
                                       LocalDateTime dataHora) {
        if (!aluno.vip) {
            throw new IllegalStateException(
                "Somente alunos VIP podem agendar aulas particulares.");
        }
        // Duas aulas inclusas em cada mês de realização das aulas.
        YearMonth mes = YearMonth.from(dataHora);
        int usadas = aluno.aulasVipPorMes.getOrDefault(mes, 0);
        double valor = usadas < 2 ? 0.0 : 80.0;
        AulaParticular aula = new AulaParticular(
            contadorAulas++, aluno, professor, dataHora, valor);
        aluno.aulasVipPorMes.put(mes, usadas + 1);
        aulasParticulares.add(aula);
        return aula;
    }

    Emprestimo emprestarLivro(Aluno aluno, Material material,
                              LocalDateTime dataHoje) {
        if (material.emprestado) {
            throw new IllegalStateException(
                "Esse material já está emprestado no momento.");
        }
        if (material.apenasVip && !aluno.vip) {
            throw new IllegalStateException(
                "Material exclusivo para assinantes VIP.");
        }
        int limite = aluno.vip ? 4 : 2;
        if (aluno.livrosComAluno().size() >= limite) {
            throw new IllegalStateException(
                "Limite de " + limite + " empréstimos simultâneos atingido.");
        }
        material.emprestado = true;
        Emprestimo emp = new Emprestimo(aluno, material, dataHoje);
        aluno.emprestimos.add(emp);
        return emp;
    }

    Pagamento cobrarMensalidade(Aluno aluno, Turma turma,
                                LocalDateTime dataPagamento) {
        double valorBruto = turma.mensalidade + (aluno.vip ? 150.0 : 0.0);
        double desconto = aluno.cursosConcluidos >= 2
            ? valorBruto * 0.10 : 0.0;
        double valorLiquido = valorBruto - desconto;
        String texto = turma.curso.idioma + " (" + turma.modalidade + ")";
        if (aluno.vip) {
            texto += " + VIP";
        }
        Pagamento pag = new Pagamento(contadorPagamentos++, texto,
            valorBruto, desconto, valorLiquido, dataPagamento);
        aluno.pagamentos.add(pag);
        return pag;
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        WorldTalk escola = new WorldTalk();
        LocalDateTime hoje = LocalDateTime.of(2026, 11, 22, 14, 0);
        Curso ing = escola.cadastrarCurso("ING01", "Inglês");
        Curso esp = escola.cadastrarCurso("ESP01", "Espanhol");
        Professor prof1 = escola.cadastrarProfessor(
            1, "Ana Clara", "Inglês");
        Professor prof2 = escola.cadastrarProfessor(
            2, "Carlos Mendez", "Espanhol");
        Aluno lucas = escola.cadastrarAluno(101, "Lucas Mendes");
        lucas.cursosConcluidos = 2;
        Aluno mariana = escola.cadastrarAluno(102, "Mariana Costa", true);
        Aluno rafael = escola.cadastrarAluno(103, "Rafael Torres", true);

        System.out.println("--- TESTE 1: MATRÍCULAS E FILA DE ESPERA ---");
        Turma turmaIng = escola.abrirTurma("T-ING-B1", ing, "Básico",
            "Presencial", "Seg/Qua 19h", prof1);
        System.out.println(turmaIng.adicionarAluno(lucas));
        System.out.println(turmaIng.adicionarAluno(mariana));
        for (int n = 0; n < 13; n++) {
            Aluno temp = escola.cadastrarAluno(
                200 + n, "Estudante " + (n + 1));
            turmaIng.adicionarAluno(temp);
        }
        Aluno bruno = escola.cadastrarAluno(104, "Bruno Silva");
        System.out.println(turmaIng.adicionarAluno(bruno));
        System.out.println(turmaIng.adicionarAluno(rafael));
        System.out.print("Fila atual: ");
        for (Aluno aluno : turmaIng.filaEspera) {
            System.out.print(aluno.nome
                + (aluno.vip ? " (VIP); " : " (Comum); "));
        }
        System.out.println();
        System.out.println(turmaIng.removerAluno(lucas));

        System.out.println("\n--- TESTE 2: APROVAÇÃO E AVANÇO DE NÍVEL ---");
        HistoricoNivel hist = mariana.boletim.get(0);
        hist.notas.addAll(Arrays.asList(8.5, 9.0, 7.5));
        hist.totalAulas = 20;
        hist.presencas = 18;
        System.out.println(mariana.fecharNivel(hist));

        System.out.println("\n--- TESTE 3: AULAS PARTICULARES ---");
        AulaParticular a1 = escola.marcarAulaParticular(
            mariana, prof1, hoje.plusDays(2));
        AulaParticular a2 = escola.marcarAulaParticular(
            mariana, prof1, hoje.plusDays(4));
        AulaParticular a3 = escola.marcarAulaParticular(
            mariana, prof1, hoje.plusHours(8));
        System.out.printf("Valor 1ª aula: R$ %.2f | "
            + "Valor 3ª aula (extra): R$ %.2f%n", a1.valor, a3.valor);
        System.out.printf("Multa por cancelar a 3ª aula "
            + "em cima da hora: R$ %.2f%n", a3.cancelar(hoje));

        System.out.println("\n--- TESTE 4: BIBLIOTECA VIRTUAL ---");
        Material livroVip = escola.adicionarMaterial(
            10, "Conversação Avançada", "Inglês", true);
        Emprestimo emp = escola.emprestarLivro(mariana, livroVip, hoje);
        System.out.println("Empréstimo: " + emp.material.titulo
            + " | Devolver até " + emp.prazoDevolucao.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        emp.devolver(hoje.plusDays(3));
        System.out.println("Livro devolvido. Ainda está emprestado? "
            + livroVip.emprestado);

        System.out.println("\n--- TESTE 5: MENSALIDADES ---");
        System.out.println(escola.cobrarMensalidade(lucas, turmaIng, hoje));
        System.out.println(escola.cobrarMensalidade(mariana, turmaIng, hoje));
    }
}
