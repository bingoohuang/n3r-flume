package org.n3r.flume.tool;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.n3r.core.lang.Substituters;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 转换从excel中贴过来的大段文本为flume配置的小工具类。
 * 以下配置：
 * 立即购买	10.142.194.155	/app/domains/malldomain/servers/MallAppServer01/logs/MallAppServer01.out
 * 10.142.194.156	/app/domains/malldomain/servers/MallAppServer02/logs/MallAppServer02.out
 * /app/domains/malldomain/servers/MallAppServer09/logs/MallAppServer09.out
 * 10.142.194.157	/app/domains/malldomain/servers/MallAppServer03/logs/MallAppServer03.out
 * /app/domains/malldomain/servers/MallAppServer10/logs/MallAppServer10.out
 * <p/>
 * <p/>
 * 号码服务	10.142.194.155	/app/domains/numdomain/servers/NumAppServer01/logs/NumAppServer01.out
 * 10.142.194.156	/app/domains/numdomain/servers/NumAppServer02/logs/NumAppServer02.out
 * 10.142.194.157	/app/domains/numdomain/servers/NumAppServer03/logs/NumAppServer03.out
 * 10.142.194.158	/app/domains/numdomain/servers/NumAppServer04/logs/NumAppServer04.out
 * <p/>
 * 需要转换为：
 * <p/>
 * agent_10_142_194_155.sources = reb1 reb2 rseq
 * agent_10_142_194_155.channels = mem
 * agent_10_142_194_155.sinks = avro
 * <p/>
 * agent_10_142_194_155.sources.reb1.type=org.n3r.flume.source.exec.ExecBlockSource
 * agent_10_142_194_155.sources.reb1.command = tail -F /app/domains/malldomain/servers/MallAppServer01/logs/MallAppServer01.out
 * agent_10_142_194_155.sources.reb1.boundaryRegex = ^\\d{2}:\\d{2}:\\d{2},\\d{3}\\s
 * agent_10_142_194_155.sources.reb1.interceptors = host static
 * agent_10_142_194_155.sources.reb1.interceptors.host.type = host
 * agent_10_142_194_155.sources.reb1.interceptors.static.type = multi_static
 * agent_10_142_194_155.sources.reb1.interceptors.static.keyValues = server:MallAppServer01 module:立即购买
 * agent_10_142_194_155.sources.reb1.channels = mem
 * <p/>
 * agent_10_142_194_155.sources.reb2.type=org.n3r.flume.source.exec.ExecBlockSource
 * agent_10_142_194_155.sources.reb2.command = tail -F /app/domains/numdomain/servers/NumAppServer01/logs/NumAppServer01.out
 * agent_10_142_194_155.sources.reb2.boundaryRegex = ^\\d{2}:\\d{2}:\\d{2},\\d{3}\\s
 * agent_10_142_194_155.sources.reb2.interceptors = host static
 * agent_10_142_194_155.sources.reb2.interceptors.host.type = host
 * agent_10_142_194_155.sources.reb2.interceptors.static.type = multi_static
 * agent_10_142_194_155.sources.reb2.interceptors.static.keyValues = server:MallAppServer01 module:号码服务
 * agent_10_142_194_155.sources.reb2.channels = mem
 * <p/>
 * agent_10_142_194_155.sources.rseq.type = seq
 * agent_10_142_194_155.sources.rseq.sleepMinMillis = 3000
 * agent_10_142_194_155.sources.rseq.sleepMaxMillis = 3000
 * agent_10_142_194_155.sources.rseq.interceptors = host
 * agent_10_142_194_155.sources.rseq.interceptors.host.type = host
 * agent_10_142_194_155.sources.rseq.channels = mem
 * <p/>
 * agent_10_142_194_155.sinks.avro.type = avro
 * agent_10_142_194_155.sinks.avro.hostname = 10.142.194.155
 * agent_10_142_194_155.sinks.avro.port = 10010
 * agent_10_142_194_155.sinks.avro.channel = cMem1
 * <p/>
 * agent_10_142_194_155.channels.mem.type = memory
 * agent_10_142_194_155.channels.mem.capacity = 100
 */
public class BatchFlumeConfGenerator {


    public static void main(String[] args) throws IOException {
        List<Line> lines = parseConf();

        Collections.sort(lines, new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                return o1.ip.compareTo(o2.ip);
            }
        });
        FileOutputStream fos = new FileOutputStream(new File("toolconf/flume-ess.properties"));
        System.setOut(new PrintStream(fos));

        String lastIp = null;
        List<Line> sameIpLines = Lists.newArrayList();
        for (Line l : lines) {
            if (lastIp == null) lastIp = l.ip;

            if (!lastIp.equals(l.ip)) {
                createConfigPartForIp(sameIpLines);
                sameIpLines.clear();
                lastIp = l.ip;
            }
            sameIpLines.add(l);
        }

        createConfigPartForIp(sameIpLines);
        fos.close();

    }

    // 匹配: [[模块名] IP] OUTFILE
    static Pattern pattern = Pattern.compile("((\\S+\\s+)?(([.\\d]+).*\\s+))?(\\S+)");
    private static List<Line> parseConf() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("toolconf/ess_tail_out.conf"), "UTF-8"));
        String line = reader.readLine();

        List<Line> lines = Lists.newArrayList();

        String module = null;
        String ip = null;
        for (; line != null; line = reader.readLine()) {
            line = StringUtils.trim(line);

            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) continue;

            if (matcher.group(2) != null) module = StringUtils.trim(matcher.group(2));
            if (matcher.group(3) != null) ip = StringUtils.trim(matcher.group(4));

            String file = matcher.group(5);
            String server = FilenameUtils.getBaseName(file);

            lines.add(new Line(module, ip, file, server));
        }
        reader.close();

        return lines;
    }

    static String firstIp = null;

    private static void createConfigPartForIp(List<Line> sameIpLines) throws IOException {
        if (sameIpLines.size() == 0) return;

        String agentName = null;
        StringBuilder sources = new StringBuilder();
        for (int i = 0, ii = sameIpLines.size(); i < ii; ++i ) {
            Line line = sameIpLines.get(i);
            if (agentName == null) agentName = "agent_" + line.ip.replaceAll("\\.", "_");

            sources.append("reb" + (i + 1) + " ");
        }

        String agentIpTemplate = null;
        if (firstIp == null) {
            firstIp = sameIpLines.get(0).ip;
            agentIpTemplate = FileUtils.readFileToString(new File("toolconf/first_collector.template"), "UTF-8");
        }
        else {
            agentIpTemplate = FileUtils.readFileToString(new File("toolconf/agent_ip.template"), "UTF-8");
        }

        Map<String, String> properties = Maps.newHashMap();
        properties.put("sources", sources.toString().trim());
        properties.put("agentName", agentName);
        properties.put("firstIp", firstIp);

        String agentIpPart = Substituters.parse(agentIpTemplate, properties);
        System.out.println(agentIpPart);
        System.out.println();

        String rebTemplate = FileUtils.readFileToString(new File("toolconf/reb_ess.template"), "UTF-8");
        for (int i = 0, ii = sameIpLines.size(); i < ii; ++i ) {
            Line line = sameIpLines.get(i);
            if (agentName == null) agentName = "agent_" + line.ip.replaceAll("\\.", "_");

            properties.put("source", "reb" + (i + 1));
            properties.put("file", line.file);
            properties.put("server", line.server);
            properties.put("module", line.module);

            String sourcePart = Substituters.parse(rebTemplate, properties);
            System.out.println(sourcePart);
            System.out.println();
        }
    }

    public static class Line {
        public String module;
        public String ip;
        public String file;
        public String server;

        public Line(String module, String ip, String file, String server) {
            this.module = module;
            this.ip = ip;
            this.file = file;
            this.server = server;
        }

        @Override
        public String toString() {
            return "Line{" +
                    "module='" + module + '\'' +
                    ", ip='" + ip + '\'' +
                    ", file='" + file + '\'' +
                    ", server='" + server + '\'' +
                    '}';
        }
    }
}
