import type { MetadataRoute } from 'next';
import { fetchAllIndividualPlants, fetchAllPublishedProducts } from '@/lib/api';

const baseUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const entries: MetadataRoute.Sitemap = [
    {
      url: `${baseUrl}/catalog`,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 1,
    },
  ];
  try {
    const [products, plants] = await Promise.all([
      fetchAllPublishedProducts(),
      fetchAllIndividualPlants(),
    ]);
    for (const product of products) {
      entries.push({
        url: `${baseUrl}/catalog/${product.id}`,
        lastModified: new Date(product.updatedAt),
        changeFrequency: 'weekly',
        priority: 0.8,
      });
    }
    for (const plant of plants) {
      entries.push({
        url: `${baseUrl}/plants/${plant.id}`,
        lastModified: new Date(plant.updatedAt),
        changeFrequency: 'weekly',
        priority: 0.7,
      });
    }
  } catch {
    // The catalog is the only static entry when the API is unavailable during generation.
  }
  return entries;
}
