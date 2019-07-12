package OGM;

import OGM.entity.EnumEntity;
import OGM.entity.MethodEntity;
import OGM.entity.TypeEntity;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Repository {
    Map<String,TypeEntity> types = new HashMap<>();
    Map<String,EnumEntity> enums = new HashMap<>();
    Map<String,MethodEntity> methods = new HashMap<>();
    SessionFactory sessionFactory;

    public Repository(){
        Configuration configuration = new Configuration.Builder().uri("bolt://162.105.88.99:11002").credentials("neo4j","neo4jpoi").build();
        sessionFactory = new SessionFactory(configuration,"OGM.entity");
        Session session = sessionFactory.openSession();
        ArrayList<TypeEntity> typesList = new ArrayList<>(session.loadAll(TypeEntity.class));
        ArrayList<EnumEntity> enumsList = new ArrayList<>(session.loadAll(EnumEntity.class));
        ArrayList<MethodEntity> methodsList = new ArrayList<>(session.loadAll(MethodEntity.class));
        for (TypeEntity typeEntity : typesList){
            types.put(typeEntity.getQualifiedName(),typeEntity);
        }
        for (EnumEntity enumEntity : enumsList){
            enums.put(enumEntity.getQualifiedName(),enumEntity);
        }
        for (MethodEntity methodEntity : methodsList){
            methods.put(methodEntity.getQualifiedSignature(),methodEntity);
        }
    }

    public TypeEntity getTyoe(String qualifiedName){
       TypeEntity type = enums.get(qualifiedName);
       if (type == null){
           type = types.get(qualifiedName);
       }
       return type;
    }

    public EnumEntity getEnum(String qualifiedName){
        return enums.get(qualifiedName);
    }

    public MethodEntity getMethod(String qualifiedSignature){
        return methods.get(qualifiedSignature);
    }


    public static void main(String[] args) {
        Repository repository = new Repository();
        TypeEntity typeEntity = repository.getTyoe("org.apache.poi.ss.usermodel.Workbook");
        Set<MethodEntity> methodEntities = typeEntity.getProducers();
        for (MethodEntity methodEntity : methodEntities){
            System.out.println(methodEntity.getSignature());
        }
        repository.sessionFactory.close();
    }
}
