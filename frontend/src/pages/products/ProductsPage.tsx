import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { productApi } from '../../services/api';
import { Product, ProductVariant } from '../../types';
import {
  Package,
  Plus,
  Tag,
  DollarSign,
  Search,
  CheckCircle,
  XCircle,
  Layers,
  ChevronDown,
  ChevronRight,
  Info,
} from 'lucide-react';

export const ProductsPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddProductModal, setShowAddProductModal] = useState(false);
  const [selectedProductForVariant, setSelectedProductForVariant] = useState<Product | null>(null);

  // New product form
  const [productForm, setProductForm] = useState({
    code: '',
    name: '',
    productType: 'SOFTWARE' as 'SOFTWARE' | 'SERVICE' | 'ADD_ON',
    salesPrice: 2000,
    costPrice: 800,
    description: '',
  });

  // New variant form
  const [variantForm, setVariantForm] = useState({
    attributeName: 'Brand',
    variantValue: 'Odoo',
    priceAdjustment: 560,
    sku: '',
  });

  const loadProducts = async () => {
    setLoading(true);
    try {
      const res = await productApi.getAll();
      setProducts(res.content);
    } catch (err) {
      console.error('Failed to load products', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await productApi.create({
        ...productForm,
        salesPrice: Number(productForm.salesPrice),
        costPrice: Number(productForm.costPrice),
      });
      setShowAddProductModal(false);
      setProductForm({
        code: '',
        name: '',
        productType: 'SOFTWARE',
        salesPrice: 2000,
        costPrice: 800,
        description: '',
      });
      await loadProducts();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create product');
    }
  };

  const handleAddVariant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProductForVariant) return;

    try {
      await productApi.addVariant(selectedProductForVariant.id, {
        attributeName: variantForm.attributeName,
        variantValue: variantForm.variantValue,
        priceAdjustment: Number(variantForm.priceAdjustment),
        sku: variantForm.sku || `${selectedProductForVariant.code}-${variantForm.variantValue.toUpperCase()}`,
      });
      setSelectedProductForVariant(null);
      setVariantForm({
        attributeName: 'Brand',
        variantValue: 'Odoo',
        priceAdjustment: 560,
        sku: '',
      });
      await loadProducts();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to add variant');
    }
  };

  const filteredProducts = products.filter(
    (p) =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.code.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Products & Configurable Variants</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Manage software, services, add-ons, and dynamic variant pricing rules (e.g. Odoo brand +₹560)
          </p>
        </div>

        {isAdmin ? (
          <button
            onClick={() => setShowAddProductModal(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-lg shadow-sm transition"
          >
            <Plus className="w-4 h-4" />
            <span>New Product</span>
          </button>
        ) : (
          <span className="px-3 py-1.5 bg-slate-100 text-slate-500 rounded-lg text-xs font-semibold border border-slate-200">
            Catalog Mode (Admin Manages Products)
          </span>
        )}
      </div>

      {/* Variant Pricing Callout */}
      <div className="p-4 bg-sky-50 border border-sky-200 rounded-xl flex items-start space-x-3 text-sky-900 text-xs">
        <Info className="w-5 h-5 text-sky-600 flex-shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-sky-800">Dynamic Variant Pricing Engine Active:</span>
          <p className="mt-0.5 text-sky-700">
            Base product prices are combined with configurable variant adjustments at billing time. For example, selecting
            <strong> Brightloop Business ERP (Base: ₹2,000)</strong> with <strong>Odoo Brand (+₹560)</strong> calculates an effective price of <strong>₹2,560</strong>.
          </p>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="relative">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search products by code or name..."
          className="w-full sm:max-w-md pl-9 pr-4 py-2 bg-white border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
        />
      </div>

      {/* Products Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase text-slate-500">
                <th className="py-3 px-4">Code</th>
                <th className="py-3 px-4">Product Name</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Sales Price</th>
                <th className="py-3 px-4">Cost Price</th>
                <th className="py-3 px-4">Configurable Variants</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredProducts.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50/70 transition">
                  <td className="py-3 px-4 font-mono font-medium text-sky-700">{p.code}</td>
                  <td className="py-3 px-4">
                    <div className="font-semibold text-slate-900">{p.name}</div>
                    <div className="text-xs text-slate-400 line-clamp-1">{p.description}</div>
                  </td>
                  <td className="py-3 px-4">
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700">
                      {p.productType}
                    </span>
                  </td>
                  <td className="py-3 px-4 font-semibold text-slate-900">₹{p.salesPrice.toLocaleString()}</td>
                  <td className="py-3 px-4 text-slate-500">₹{p.costPrice.toLocaleString()}</td>
                  <td className="py-3 px-4">
                    {p.variants && p.variants.length > 0 ? (
                      <div className="flex flex-wrap gap-1.5">
                        {p.variants.map((v) => (
                          <span
                            key={v.id}
                            className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-indigo-50 text-indigo-700 border border-indigo-200"
                          >
                            {v.attributeName}: <strong>{v.variantValue}</strong>
                            {v.priceAdjustment !== 0 && (
                              <span className="ml-1 text-emerald-700 font-bold">
                                {v.priceAdjustment > 0 ? `+₹${v.priceAdjustment}` : `-₹${Math.abs(v.priceAdjustment)}`}
                              </span>
                            )}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-xs text-slate-400 italic">No variants configured</span>
                    )}
                  </td>
                  <td className="py-3 px-4 text-right">
                    {isAdmin && (
                      <button
                        onClick={() => setSelectedProductForVariant(p)}
                        className="px-2.5 py-1 text-xs font-semibold text-sky-600 hover:text-sky-700 hover:bg-sky-50 rounded border border-sky-200 transition"
                      >
                        + Add Variant
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal: Create Product */}
      {showAddProductModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Create New Catalog Product</h2>
            <form onSubmit={handleCreateProduct} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Product Code</label>
                  <input
                    type="text"
                    required
                    value={productForm.code}
                    onChange={(e) => setProductForm({ ...productForm, code: e.target.value })}
                    placeholder="PROD-CRM"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Type</label>
                  <select
                    value={productForm.productType}
                    onChange={(e: any) => setProductForm({ ...productForm, productType: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  >
                    <option value="SOFTWARE">SOFTWARE</option>
                    <option value="SERVICE">SERVICE</option>
                    <option value="ADD_ON">ADD_ON</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Product Name</label>
                <input
                  type="text"
                  required
                  value={productForm.name}
                  onChange={(e) => setProductForm({ ...productForm, name: e.target.value })}
                  placeholder="Brightloop CRM Enterprise"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Sales Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={productForm.salesPrice}
                    onChange={(e) => setProductForm({ ...productForm, salesPrice: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Cost Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={productForm.costPrice}
                    onChange={(e) => setProductForm({ ...productForm, costPrice: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Description</label>
                <textarea
                  rows={2}
                  value={productForm.description}
                  onChange={(e) => setProductForm({ ...productForm, description: e.target.value })}
                  placeholder="Product description and subscription notes..."
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddProductModal(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-lg"
                >
                  Create Product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Variant */}
      {selectedProductForVariant && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900 mb-1">
              Add Variant for {selectedProductForVariant.name}
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              Base Price: ₹{selectedProductForVariant.salesPrice.toLocaleString()}
            </p>

            <form onSubmit={handleAddVariant} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Attribute Name</label>
                  <input
                    type="text"
                    required
                    value={variantForm.attributeName}
                    onChange={(e) => setVariantForm({ ...variantForm, attributeName: e.target.value })}
                    placeholder="Brand / Edition"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">Variant Value</label>
                  <input
                    type="text"
                    required
                    value={variantForm.variantValue}
                    onChange={(e) => setVariantForm({ ...variantForm, variantValue: e.target.value })}
                    placeholder="Odoo"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                  Price Adjustment (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={variantForm.priceAdjustment}
                  onChange={(e) => setVariantForm({ ...variantForm, priceAdjustment: Number(e.target.value) })}
                  placeholder="560.00"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm"
                />
                <p className="text-xs text-slate-400 mt-1">
                  Calculated effective price:{' '}
                  <strong>
                    ₹
                    {(
                      selectedProductForVariant.salesPrice + Number(variantForm.priceAdjustment || 0)
                    ).toLocaleString()}
                  </strong>
                </p>
              </div>

              <div className="flex items-center justify-end space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setSelectedProductForVariant(null)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-500 rounded-lg"
                >
                  Save Variant
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
