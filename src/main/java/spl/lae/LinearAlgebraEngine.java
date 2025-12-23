package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        computationRoot.associativeNesting();
        ComputationNode currNode = computationRoot.findResolvable();
        while (currNode != null) {
            loadAndCompute(currNode);
            currNode = computationRoot.findResolvable();
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor;   
        if (node == null) {
            throw new IllegalArgumentException("ComputationNode is null");
        }
        List<Runnable> tasks = new ArrayList<>();
        switch (node.getNodeType()) {
            case ADD:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix()); 
                rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix()); 
                tasks = createAddTasks();
                executor.submitAll(tasks);
                node.resolve(leftMatrix.readRowMajor());
                break;
            case MULTIPLY:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix()); 
                rightMatrix.loadColumnMajor(node.getChildren().get(1).getMatrix()); 
                tasks = createMultiplyTasks();
                executor.submitAll(tasks);
                node.resolve(leftMatrix.readRowMajor());
                break;
            case NEGATE:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix()); 
                tasks = createNegateTasks();
                executor.submitAll(tasks);
                node.resolve(leftMatrix.readRowMajor());
                break;
            case TRANSPOSE:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix()); 
                tasks = createTransposeTasks();
                executor.submitAll(tasks);
                node.resolve(leftMatrix.readRowMajor());
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
        }
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        if (leftMatrix == null || rightMatrix == null ||
            leftMatrix.get(0).length() == 0 || rightMatrix.get(0).length() == 0 ||
            leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR ||  rightMatrix.getOrientation() != VectorOrientation.ROW_MAJOR ||
             leftMatrix.length() != rightMatrix.length()) {
            throw new IllegalArgumentException("can't perform addition");
        }

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0 ; i < leftMatrix.length(); i = i + 1) {
            final int rowIndex = i;
            tasks.add( () -> {
                leftMatrix.get(rowIndex).add(rightMatrix.get(rowIndex));
            } );
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        if (leftMatrix == null || rightMatrix == null ||
            leftMatrix.get(0).length() == 0 || rightMatrix.get(0).length() == 0 ||
            leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR ||
        (rightMatrix.getOrientation() == VectorOrientation.ROW_MAJOR && leftMatrix.get(0).length() != rightMatrix.length()) ||
        (rightMatrix.getOrientation() == VectorOrientation.COLUMN_MAJOR && leftMatrix.get(0).length() != rightMatrix.get(0).length())) {
            throw new IllegalArgumentException("can't perform multiplication");
        }

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0 ; i < leftMatrix.length(); i = i + 1) {
            final int rowIndex = i;
            tasks.add( () -> {
                leftMatrix.get(rowIndex).vecMatMul(rightMatrix);
            });
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        if (leftMatrix == null || leftMatrix.get(0).length() == 0 
         || leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("matrix is null or not valid");
        }
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0 ; i < leftMatrix.length(); i = i + 1) {
            final int rowIndex = i;
            tasks.add( () -> {
                leftMatrix.get(rowIndex).negate();
            });
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        if (leftMatrix == null || leftMatrix.get(0).length() == 0 
         || leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR ) {
            throw new IllegalArgumentException("matrix is null or not valid");
        }       
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0 ; i < leftMatrix.length(); i = i + 1) {
            final int rowIndex = i;
            tasks.add( () -> {
                leftMatrix.get(rowIndex).transpose();
            });
        }
        
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }
}
