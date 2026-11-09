import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AdminLayout } from '@/layouts/AdminLayout';
import { Dashboard } from '@/pages/Dashboard';
import { Books } from '@/pages/Books';
import { Categories } from '@/pages/Categories';
import { Orders } from '@/pages/Orders';
import { Inventory } from '@/pages/Inventory';
import { CustomersStaff } from '@/pages/CustomersStaff';
import { Vouchers } from '@/pages/Vouchers';
import { ReportsSettings } from '@/pages/ReportsSettings';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<AdminLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="books" element={<Books />} />
        <Route path="categories" element={<Categories />} />
        <Route path="orders" element={<Orders />} />
        <Route path="inventory" element={<Inventory />} />
        <Route path="customers" element={<CustomersStaff />} />
        <Route path="vouchers" element={<Vouchers />} />
        <Route path="reports" element={<ReportsSettings />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
};

