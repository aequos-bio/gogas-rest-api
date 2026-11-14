import { useCallback, useState } from 'react';
import { useSnackbar } from 'notistack';
import { apiDelete, apiGetJson, apiPost, apiPut } from '../../../utils/axios_utils';
import { downloadFile } from '../../../utils/download_utils';
import { apiPostMultipart } from '../../../utils/upload_utils';
import { ErrorResponse } from '../../../store/types';
import { Product, ProductInput, SelectItem, SynchroInfo } from './types';

const errorText = (err: any, fallback: string) =>
  err?.response?.data
    ? err.response.data.debugMessage || err.response.data.message || err.response.data.error
    : `${fallback}: ${err}`;

const isError = (res: any): res is ErrorResponse =>
  res && typeof res === 'object' && (res as ErrorResponse).error;

/**
 * Data hook for the Products (Gestione prodotti) page.
 *
 * Endpoints:
 *  - GET  /api/ordertype/select/manager           (order types, manager scope)
 *  - GET  /api/category/list/{typeId}             (categories for a type)
 *  - GET  /api/supplier/select                    (suppliers)
 *  - GET  /api/ordertype/{typeId}/synchro/info    (aequos link + last sync)
 *  - GET  /api/products/list/{typeId}?category&available&cancelled  (search)
 *  - POST /api/products, PUT/DELETE /api/products/{id}              (CRUD)
 *  - PUT  /api/products/{typeId}/sync             (sync price list with Aequos)
 *  - POST /api/products/{typeId}/import           (import excel price list)
 *  - GET  /api/products/list/{typeId}/export      (export excel price list)
 */
export const useProductsAPI = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [orderTypes, setOrderTypes] = useState<SelectItem[]>([]);
  const [suppliers, setSuppliers] = useState<SelectItem[]>([]);
  const [loading, setLoading] = useState(false);
  const { enqueueSnackbar } = useSnackbar();

  const loadOrderTypes = useCallback(() => {
    apiGetJson<SelectItem[] | ErrorResponse>('/api/ordertype/select/manager', { firstEmpty: false }).then(
      (res) => {
        if (!isError(res)) setOrderTypes(res as SelectItem[]);
      },
    );
  }, []);

  const loadSuppliers = useCallback(() => {
    apiGetJson<SelectItem[] | ErrorResponse>('/api/supplier/select', {}).then((res) => {
      if (!isError(res)) setSuppliers(res as SelectItem[]);
    });
  }, []);

  const loadCategories = useCallback((typeId: string): Promise<SelectItem[]> => {
    if (!typeId) return Promise.resolve([]);
    return apiGetJson<SelectItem[] | ErrorResponse>(`/api/category/list/${typeId}`, {}).then((res) =>
      isError(res) ? [] : (res as SelectItem[]),
    );
  }, []);

  const getSynchroInfo = useCallback((typeId: string): Promise<SynchroInfo | undefined> => {
    if (!typeId) return Promise.resolve(undefined);
    return apiGetJson<SynchroInfo | ErrorResponse>(`/api/ordertype/${typeId}/synchro/info`, {}).then((res) =>
      isError(res) ? undefined : (res as SynchroInfo),
    );
  }, []);

  const search = useCallback(
    (typeId: string, category?: string, available?: string, cancelled?: string) => {
      if (!typeId) {
        setProducts([]);
        return;
      }
      setLoading(true);
      const params: Record<string, string> = {};
      if (category) params.category = category;
      if (available) params.available = available;
      if (cancelled) params.cancelled = cancelled;

      apiGetJson<Product[] | ErrorResponse>(`/api/products/list/${typeId}`, params)
        .then((res) => {
          setLoading(false);
          if (isError(res)) {
            enqueueSnackbar((res as ErrorResponse).errorMessage, { variant: 'error' });
          } else {
            setProducts(res as Product[]);
          }
        })
        .catch((err) => {
          setLoading(false);
          enqueueSnackbar(errorText(err, 'Errore durante la ricerca dei prodotti'), { variant: 'error' });
        });
    },
    [enqueueSnackbar],
  );

  const createProduct = useCallback(
    (product: ProductInput): Promise<boolean> =>
      apiPost('/api/products', product)
        .then(() => {
          enqueueSnackbar('Prodotto creato con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la creazione del prodotto'), { variant: 'error' });
          return false;
        }),
    [enqueueSnackbar],
  );

  const updateProduct = useCallback(
    (id: string, product: ProductInput): Promise<boolean> =>
      apiPut(`/api/products/${id}`, product)
        .then(() => {
          enqueueSnackbar('Prodotto modificato con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la modifica del prodotto'), { variant: 'error' });
          return false;
        }),
    [enqueueSnackbar],
  );

  const deleteProduct = useCallback(
    (id: string): Promise<boolean> =>
      apiDelete(`/api/products/${id}`)
        .then(() => {
          enqueueSnackbar('Prodotto eliminato con successo', { variant: 'success' });
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'eliminazione del prodotto"), { variant: 'error' });
          return false;
        }),
    [enqueueSnackbar],
  );

  const toggleAvailability = useCallback(
    (product: Product): Promise<boolean> => {
      const payload: ProductInput = {
        descrizione: product.descrizione,
        unitamisura: product.unitamisura,
        prezzounitario: product.prezzounitario,
        unitamisuracollo: product.unitamisuracollo,
        pesocassa: product.pesocassa,
        cadenza: product.cadenza,
        codiceesterno: product.codiceesterno,
        note: product.note,
        idproduttore: product.idproduttore,
        idtipo: product.idtipo,
        idcategoria: product.idcategoria,
        ordinabile: !product.ordinabile,
        annullato: product.annullato,
        solocollointero: product.solocollointero,
        multiplo: product.multiplo ?? null,
      };
      return apiPut(`/api/products/${product.id}`, payload)
        .then(() => {
          enqueueSnackbar(
            payload.ordinabile ? 'Prodotto aggiunto al listino' : 'Prodotto rimosso dal listino',
            { variant: 'success' },
          );
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, "Errore durante l'aggiornamento della disponibilità"), {
            variant: 'error',
          });
          return false;
        });
    },
    [enqueueSnackbar],
  );

  const syncPriceList = useCallback(
    (typeId: string): Promise<SynchroInfo | undefined> =>
      apiPut(`/api/products/${typeId}/sync`)
        .then((res) => {
          enqueueSnackbar('Sincronizzazione effettuata con successo', { variant: 'success' });
          return res.data as SynchroInfo;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante la sincronizzazione del listino'), {
            variant: 'error',
          });
          return undefined;
        }),
    [enqueueSnackbar],
  );

  const importPriceList = useCallback(
    (typeId: string, file: File): Promise<boolean> => {
      const formData = new FormData();
      formData.append('file', file);
      return apiPostMultipart(`/api/products/${typeId}/import`, formData)
        .then((res) => {
          const updated = res.data?.updatedProducts;
          enqueueSnackbar(
            `Listino caricato con successo${updated != null ? ` (${updated} articoli)` : ''}`,
            { variant: 'success' },
          );
          return true;
        })
        .catch((err) => {
          enqueueSnackbar(errorText(err, 'Errore durante il caricamento del listino'), { variant: 'error' });
          return false;
        });
    },
    [enqueueSnackbar],
  );

  const exportPriceList = useCallback(
    (typeId: string): Promise<void> =>
      downloadFile(`/api/products/list/${typeId}/export`, 'listino.xlsx').catch((err) => {
        enqueueSnackbar(errorText(err, "Errore durante l'esportazione del listino"), { variant: 'error' });
      }),
    [enqueueSnackbar],
  );

  return {
    products,
    orderTypes,
    suppliers,
    loading,
    loadOrderTypes,
    loadSuppliers,
    loadCategories,
    getSynchroInfo,
    search,
    createProduct,
    updateProduct,
    deleteProduct,
    toggleAvailability,
    syncPriceList,
    importPriceList,
    exportPriceList,
  };
};
