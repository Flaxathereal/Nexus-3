package de.nexus.app

import org.mozilla.javascript.*
import org.mozilla.javascript.Function as RhinoFunction
import java.io.File
import java.net.URL

class Host(private val root: File, private val mem: Memory, private val test: Boolean) {
    private val data = File(root, "daten").apply { mkdirs() }
    private fun safe(base: File, rel: String): File {
        val f = File(base, rel).canonicalFile
        require(f.path.startsWith(base.canonicalPath)) { "Pfad nicht erlaubt" }
        return f
    }
    fun log(s: String) {}
    fun readFile(rel: String): String = safe(data, rel).readText()
    fun writeFile(rel: String, t: String) { if (!test) safe(data, rel).apply { parentFile?.mkdirs() }.writeText(t) }
    fun remember(t: String) { if (!test) mem.add(t) }
    fun recall(q: String): String = mem.recall(q).joinToString("\n")
    fun httpGet(u: String): String {
        val c = URL(u).openConnection(); c.connectTimeout = 8000; c.readTimeout = 8000
        return c.getInputStream().bufferedReader().use { it.readText() }.take(20000)
    }
}

class TimedFactory : ContextFactory() {
    @Volatile var start = 0L
    override fun makeContext(): Context {
        val cx = super.makeContext(); cx.optimizationLevel = -1; cx.instructionObserverThreshold = 10000; return cx
    }
    override fun observeInstructionCount(cx: Context, n: Int) {
        if (System.currentTimeMillis() - start > 5000) throw Error("Zeitlimit")
    }
}

class SkillEngine(private val root: File, private val mem: Memory) {
    private val dir = File(root, "skills").apply { mkdirs() }
    private val bak = File(dir, "backup").apply { mkdirs() }
    private val fac = TimedFactory()

    private fun exec(code: String, fn: String, arg: String?, test: Boolean): String {
        val cx = fac.enterContext(); fac.start = System.currentTimeMillis()
        try {
            val scope = cx.initSafeStandardObjects()
            ScriptableObject.putProperty(scope, "host", Context.javaToJS(Host(root, mem, test), scope))
            cx.evaluateString(scope, code, "skill", 1, null)
            val f = scope.get(fn, scope)
            if (f !is RhinoFunction) throw IllegalStateException("Funktion $fn fehlt")
            val args: Array<Any?> = if (arg == null) emptyArray() else arrayOf(arg)
            return Context.toString(f.call(cx, scope, scope, args))
        } finally { Context.exit() }
    }

    fun test(code: String): String? = try {
        val r = exec(code, "test", null, true)
        if (r == "true") null else "test() lieferte '$r' statt true"
    } catch (e: Throwable) { e.message ?: e.toString() }

    fun install(name: String, code: String): String? {
        test(code)?.let { return it }
        val f = File(dir, "$name.js")
        if (f.exists()) f.copyTo(File(bak, "${name}_${System.currentTimeMillis()}.js"), true)
        f.writeText(code); return null
    }

    fun rollback(name: String): Boolean {
        val b = bak.listFiles()?.filter { it.name.startsWith(name + "_") }?.maxByOrNull { it.name } ?: return false
        b.copyTo(File(dir, "$name.js"), true); return true
    }

    fun list(): List<Pair<String, String>> = dir.listFiles()?.filter { it.extension == "js" }?.map {
        it.nameWithoutExtension to (Regex("//\\s*beschreibung:\\s*(.*)").find(it.readText())?.groupValues?.get(1) ?: "")
    } ?: emptyList()

    fun run(name: String, arg: String): String {
        require(Regex("[a-z0-9_]{1,30}").matches(name)) { "Unbekannte Fähigkeit" }
        return exec(File(dir, "$name.js").readText(), "run", arg, false)
    }
}
