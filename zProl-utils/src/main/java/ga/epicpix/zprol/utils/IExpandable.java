package ga.epicpix.zprol.utils;

import java.util.List;

public interface IExpandable<T> {

    public T expandItem();

    public static void showTree(IExpandable<?> expandable) {
        System.out.println("<root>");
        System.out.println(generateTree(expandable));
    }

    public static String generateTree(IExpandable<?> expandable) {
        if(expandable == null) {
            return "└─ <???>";
        }
        Object expanded = expandable.expandItem();
        while(expanded instanceof IExpandable<?> exp) expanded = exp.expandItem();

        if(expanded instanceof List list) {
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i<list.size(); i++) {
                Object get = list.get(i);
                if(get instanceof IExpandable exp) {
                    Object xx = exp.expandItem();
                    String[] s = generateTree(exp).split("\n");
                    for(int j = 0; j<s.length; j++) {
                        if(xx instanceof List) {
                            if(j == 0) {
                                if(list.size() - 1 == i) builder.append("└─");
                                else builder.append("├─");
                            } else {
                                if(list.size() - 1 != i) builder.append("│ ");
                                else builder.append("  ");
                            }
                        }else {
                            if(i == 0) {
                                if(list.size() == 1) builder.append("──");
                                else builder.append("┬─");
                            }else {
                                if(list.size() - 1 == i) builder.append("└─");
                                else builder.append("├─");
                            }
                        }
                        builder.append(s[j]).append("\n");
                    }
                }else {
                    builder.append(get);
                }
            }
            return builder.toString();
        }

        return " " + expanded;
    }

}
