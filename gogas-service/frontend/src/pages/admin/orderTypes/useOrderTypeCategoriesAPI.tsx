import { useCallback, useState } from "react";
import { apiDelete, apiGetJson, apiPost, apiPut, apiPostText } from "../../../utils/axios_utils";
import { orderBy } from "lodash";
import { Category } from "./typed";
import { ErrorResponse } from "../../../store/types";
import { useSnackbar } from "notistack";

export const useOrderTypeCategoriesAPI = (orderTypeId?: string) => {
  const { enqueueSnackbar } = useSnackbar();
  const [categories, setCategories] = useState<Category[]>([]);

  const reload = useCallback(() => {
    if (orderTypeId) {
      apiGetJson<Category[] | ErrorResponse>(`/api/category/list/${orderTypeId}`, {}).then((categoryList) => {
        if (typeof categoryList === 'object' && (categoryList as ErrorResponse).error) {
          enqueueSnackbar((categoryList as ErrorResponse).errorMessage, { variant: 'error' });
        } else {
          setCategories(orderBy((categoryList as Category[]), 'description'));
        }
      });
    }
  }, [orderTypeId]);

  const createCategory = useCallback((description: string): Promise<void> => {
    return apiPostText(`/api/category/${orderTypeId}`, description)
      .then(() => {
        enqueueSnackbar('Nuova categoria creata', { variant: 'success' });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nella creazione della categoria',
          { variant: 'error' },
        );
      });
  }, [orderTypeId]);

  const updateCategory = useCallback((id: string, description: string): Promise<void> => {
    return apiPut(`/api/category/${orderTypeId}`, {
      id,
      description,
    })
      .then(() => {
        enqueueSnackbar('Categoria modificata', { variant: 'success' });
        reload();
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText || 'Errore nella modifica della categoria',
          { variant: 'error' },
        );
      });
  }, [orderTypeId]);

  const deleteCategory = useCallback((category: Category): Promise<void> => {
    return apiDelete(`/api/category/${category.id}`)
      .then(() => {
        reload();
        enqueueSnackbar('Categora eliminata', { variant: 'success' });
      })
      .catch((err) => {
        enqueueSnackbar(
          err.response?.statusText ||
          "Errore nell'eliminazione della categoria",
          { variant: 'error' },
        );
      });
  }, []);

  return { categories, reload, createCategory, updateCategory, deleteCategory }
}