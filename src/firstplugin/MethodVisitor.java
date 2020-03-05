package firstplugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodVisitor extends ASTVisitor{
	private List<AnnotationTypeDeclaration> annotations=new ArrayList<>();
	private List<MethodDeclaration> methods = new ArrayList<>();
	private List<TypeDeclaration> types=new ArrayList<>();
	
	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		annotations.add(node);
		return super.visit(node);
	}
	
	@Override
	public boolean visit(TypeDeclaration node) {
		types.add(node);
		return super.visit(node);
	}
	

    @Override
    public boolean visit(MethodDeclaration node) {
        methods.add(node);
        return super.visit(node);
    }

    public List<MethodDeclaration> getMethods() {
        return methods;
    }
    
    public List<AnnotationTypeDeclaration> getAnnotations(){
    	return annotations;
    }

	public List<TypeDeclaration> getTypes() {
		return types;
	}
}
