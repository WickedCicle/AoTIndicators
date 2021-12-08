import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.demidko.aot.WordformMeaning.lookupForMeanings;

public class Example {

    public static void main(String[] args) throws Exception {
        
        HashMap<String, String> tags = new HashMap<>();

        fillTags(tags);

        ArrayList<String> texts = new ArrayList<>();

        Files.walk(Paths.get("D:\\Downloads\\RNC_million\\RNC_million\\sample_ar\\TEXTS"))
                .filter(Files::isRegularFile)
                .forEach((file -> {
                    texts.add(file.toString());
                }));

        AtomicInteger intUnfamilliar = new AtomicInteger(); // ненайденные в словаре
        AtomicInteger intKnown = new AtomicInteger(); // найденные в словаре
        AtomicInteger wordCount = new AtomicInteger(); // суммарное количесвто слов
        AtomicInteger accuracy = new AtomicInteger(); // точно определённые слова
        AtomicInteger morphAccuracy = new AtomicInteger(); // первая же форма с подходящими морфологическими хар-ками
        AtomicBoolean isAdded = new AtomicBoolean(false);

        Instant start;
        Instant finish;
        long elapsed = 0;

        try {
            for (String text : texts) {
                System.out.println("next file" + text);
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(text);

                Node html = document.getDocumentElement();

                NodeList htmlProps = html.getChildNodes();
                for (int i = 0; i < htmlProps.getLength(); i++) {
                    Node body = htmlProps.item(i);
                    if (body.getNodeType() != Node.TEXT_NODE && body.getNodeName().equals("body")) {
                        NodeList bodyProps = body.getChildNodes();
                        for (int j = 0; j < bodyProps.getLength(); j++) {
                            Node paragraph = bodyProps.item(j);
                            if (paragraph.getNodeType() != Node.TEXT_NODE && (paragraph.getNodeName().equals("p") || paragraph.getNodeName().equals("speach"))) {
                                NodeList paragraphProps = paragraph.getChildNodes();
                                for (int k = 0; k < paragraphProps.getLength(); k++) {
                                    Node sentence = paragraphProps.item(k);
                                    if (sentence.getNodeType() != Node.TEXT_NODE && sentence.getNodeName().equals("se")) {
                                        NodeList sentenceProps = sentence.getChildNodes();
                                        for (int m = 0; m < sentenceProps.getLength(); m++) {
                                            Node word = sentenceProps.item(m);
                                            if (word.getNodeType() != Node.TEXT_NODE && word.getNodeName().equals("w")) {
                                                wordCount.getAndIncrement();
                                                NodeList wordProps = word.getChildNodes();
                                                start = Instant.now();
                                                var meanings = lookupForMeanings(word.getTextContent().toLowerCase(Locale.ROOT).replaceAll("[` ]", ""));
                                                for (int n = 0; n < wordProps.getLength(); n++) {
                                                    Node characteristics = wordProps.item(n);
                                                    if (isAdded.get()) {
                                                        continue;
                                                    }
                                                    if (characteristics.getNodeType() != Node.TEXT_NODE && characteristics.getNodeName().equals("ana")) {
                                                        if (meanings.size() != 0) {
                                                            intKnown.getAndIncrement();
                                                            if (Objects.equals(characteristics.getAttributes().getNamedItem("lex").getNodeValue().toLowerCase(Locale.ROOT).replaceAll("ё", "е"), meanings.get(0).getLemma().toString())){
                                                                accuracy.getAndIncrement();
                                                            }

                                                            isAdded.set(true);

                                                            StringBuilder transformTag = new StringBuilder();

                                                            for (int q = 0; q < meanings.get(0).getMorphology().size(); q++){
                                                                transformTag.append(meanings.get(0).getMorphology().get(q));
                                                                transformTag.append(",");
                                                            }

                                                            transformTag = new StringBuilder(transformTag.substring(0, transformTag.length() - 1));

                                                            String[] transformTagSplit = transformTag.toString().split("[,]");
                                                            StringBuilder temp = new StringBuilder();

                                                            for (String value : transformTagSplit) {
                                                                temp.append(tags.get(value));
                                                                temp.append(",");
                                                            }
                                                            temp = new StringBuilder(temp.substring(0, temp.length() - 1));

                                                            String[] transformedTag = temp.toString().split(",");

                                                            List<String> list = new ArrayList<>();
                                                            for (String s : transformedTag) {
                                                                if (s != null && !Objects.equals(s, "null") && !s.equals("0") && s.length() > 0) {
                                                                    list.add(s);
                                                                }
                                                            }
                                                            transformedTag = list.toArray(new String[0]);

                                                            String[] markTags = characteristics.getAttributes().getNamedItem("gr").getNodeValue()
                                                                    .replaceAll("-PRO", "").replaceAll("PRO", "")
                                                                    .replaceAll("distort", "").replaceAll("persn", "")
                                                                    .replaceAll("patrn", "").replaceAll("indic", "")
                                                                    .replaceAll("imper", "").replaceAll("abbr", "")
                                                                    .replaceAll("ciph", "").replaceAll("INIT", "")
                                                                    .replaceAll("anom", "").replaceAll("famn", "")
                                                                    .replaceAll("zoon", "").replaceAll("pass", "")
                                                                    .replaceAll("inan", "").replaceAll("anim", "")
                                                                    .replaceAll("intr", "").replaceAll("tran", "")
                                                                    .replaceAll("act", "").replaceAll("ipf", "")
                                                                    .replaceAll("med", "").replaceAll("pf", "")
                                                                    .split("[,=]");

                                                            list = new ArrayList<>();
                                                            for (String s : markTags) {
                                                                if (s != null && !Objects.equals(s, "null") && !s.equals("0") && s.length() > 0) {
                                                                    list.add(s);
                                                                }
                                                            }
                                                            markTags = list.toArray(new String[0]);

                                                            for (String markTag : markTags) {
                                                                if (!Arrays.asList(transformedTag).contains(markTag)) {
                                                                    isAdded.set(false);
                                                                }
                                                            }

                                                            if (isAdded.get()){
                                                                morphAccuracy.getAndIncrement();
                                                            }
                                                        } else {
                                                            intUnfamilliar.getAndIncrement();
                                                        }
                                                        isAdded.set(true);
                                                    }
                                                }
                                                finish = Instant.now();
                                                elapsed += Duration.between(start, finish).toMillis();
                                                isAdded.set(false);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Количество ненайдённых: " + intUnfamilliar);
            System.out.println("Количество найдённых в словаре: " + intKnown);
            System.out.println("Общее количество слов: " + wordCount);
            System.out.println("Точно определенных начальных форм слов: " + accuracy);
            System.out.println("Точно определенных форм слов с полными характеристиками: " + morphAccuracy);
            System.out.println("Процент ненайдённых:" + intUnfamilliar.doubleValue()/wordCount.doubleValue());
            System.out.println("Точность начальных форм: " + accuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Точность определения характеристик первой формы: " + morphAccuracy.doubleValue()/intKnown.doubleValue());
            System.out.println("Затраченное время: " + (double)elapsed/1000 + " секунд");


        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace(System.out);
        }
    }
    
    static void fillTags(HashMap<String, String> tags) {
        tags.put("С", "S");
        tags.put("мр", "m");
        tags.put("ед", "sg");
        tags.put("им", "nom");
        tags.put("рд", "gen");
        tags.put("дт", "dat");
        tags.put("вн", "acc");
        tags.put("тв", "ins");
        tags.put("пр", "loc");
        tags.put("зв", "voc");
        tags.put("мн", "pl");
        tags.put("мр-жр", "m-f");
        tags.put("жр", "f");
        tags.put("ср", "n");
        tags.put("П", "A,plen");
        tags.put("сравн", "comp");
        tags.put("од", "anim");
        tags.put("но", "inan");
        tags.put("прев", "supr");
        tags.put("ИНФИНИТИВ", "V,inf");
        tags.put("Г", "V");
        tags.put("нст", "praes");
        tags.put("прш", "praet");
        tags.put("буд", "fut");
        tags.put("1л", "1p");
        tags.put("2л", "2p");
        tags.put("3л", "3p");
        tags.put("ДЕЕПРИЧАСТИЕ", "V,ger");
        tags.put("пвл", "imper");
        tags.put("ПРИЧАСТИЕ", "V,partcp,plen");
        tags.put("КР_ПРИЧАСТИЕ", "V,partcp,brev");
        tags.put("МС", "S");
        tags.put("МС-ПРЕДК", "PRAEDIC");
        tags.put("МС-П", "A");
        tags.put("ЧИСЛ", "NUM");
        tags.put("ЧИСЛ-П", "ANUM");
        tags.put("Н", "ADV");
        tags.put("ПРЕДК", "PRAEDIC");
        tags.put("ПРЕДЛ", "PR");
        tags.put("СОЮЗ", "CONJ");
        tags.put("МЕЖД", "INTJ");
        tags.put("ЧАСТ", "PART");
        tags.put("ВВОДН", "PARENTH");
        tags.put("св", "pf");
        tags.put("нс", "ipf");
        tags.put("пе", "tran");
        tags.put("нп", "intr");
        tags.put("указат", "indic");
    }
}