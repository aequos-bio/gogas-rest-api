package eu.aequos.gogas.utils;

public class RestResponse<T> {
  private T data;
  private boolean error;
  private String errorMessage;

  public RestResponse(T data) {
      this.data = data;
  }

  public RestResponse(Exception ex) {
      error = true;
      errorMessage = ex.getMessage();
  }

  public T getData() {
      return data;
  }

  public void setData(T data) {
      this.data = data;
  }

  public boolean isError() {
      return error;
  }

  public void setError(boolean error) {
      this.error = error;
  }

  public String getErrorMessage() {
      return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
  }
}
