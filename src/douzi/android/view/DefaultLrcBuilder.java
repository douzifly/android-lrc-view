/**
 * douzifly @Aug 10, 2013
 * github.com/douzifly
 * douzifly@gmail.com
 */
package douzi.android.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

/** default lrc builder,convert raw lrc string to lrc rows */
public class DefaultLrcBuilder implements  ILrcBuilder{
    static final String TAG = "DefaultLrcBuilder";
    public List<LrcRow> getLrcRows(String rawLrc) {
        Log.d(TAG,"getLrcRows by rawString");
        if(rawLrc == null || rawLrc.length() == 0){
            Log.e(TAG,"getLrcRows rawLrc null or empty");
            return null;
        }
        StringReader reader = new StringReader(rawLrc);
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        List<LrcRow> rows = new ArrayList<LrcRow>();
        try{
            do{
                line = br.readLine();
                Log.d(TAG,"lrc raw line:" + line);
                if(line != null && line.length() > 0){
                    List<LrcRow> lrcRows = LrcRow.createRows(line);
                    if(lrcRows != null && lrcRows.size() > 0){
                        for(LrcRow row : lrcRows){
                            rows.add(row);
                        }
                    }
                }
                
            }while(line != null);
            if( rows.size() > 0 ){
                // sort by time:
                Collections.sort(rows);
            }
            
        }catch(Exception e){
            Log.e(TAG,"parse exceptioned:" + e.getMessage());
            return null;
        }finally{
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            reader.close();
        }
        return rows;
    }
}
