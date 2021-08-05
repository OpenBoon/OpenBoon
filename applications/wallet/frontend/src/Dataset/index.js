import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Breadcrumbs from '../Breadcrumbs'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import DatasetConcepts from '../DatasetConcepts'
import ConceptEdit from '../ConceptEdit'
import DatasetLabels from '../DatasetLabels'
import DatasetModels from '../DatasetModels'

import DatasetDetails from './Details'

const Dataset = () => {
  const {
    pathname,
    query: { projectId, datasetId, edit = '', action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Dataset Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          {
            title: 'Datasets',
            href: '/[projectId]/datasets',
            isBeta: true,
          },
          { title: 'Dataset Details', href: false },
        ]}
      />

      {!!action && (
        <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            {action === 'edit-dataset-success' && 'Dataset updated.'}
            {action === 'edit-concept-success' && 'Concept updated.'}
            {action === 'delete-concept-success' && 'Concept deleted.'}
            {action === 'remove-asset-success' &&
              'Asset has been removed from dataset.'}
          </FlashMessage>
        </div>
      )}

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <DatasetDetails
          key={pathname}
          projectId={projectId}
          datasetId={datasetId}
        />

        <Tabs
          tabs={[
            {
              title: 'Concepts',
              href: '/[projectId]/datasets/[datasetId]',
              isSelected: edit ? false : undefined,
            },
            {
              title: 'Labeled Assets',
              href: '/[projectId]/datasets/[datasetId]/labels',
              isSelected: edit ? false : undefined,
            },
            {
              title: 'Linked Models',
              href: '/[projectId]/datasets/[datasetId]/models',
              isSelected: edit ? false : undefined,
            },
            edit
              ? {
                  title: 'Edit Concept',
                  href: '/[projectId]/datasets/[datasetId]',
                  isSelected: true,
                }
              : {},
          ]}
        />

        {pathname === '/[projectId]/datasets/[datasetId]' &&
          (edit ? (
            <ConceptEdit
              projectId={projectId}
              datasetId={datasetId}
              label={edit}
            />
          ) : (
            <DatasetConcepts
              projectId={projectId}
              datasetId={datasetId}
              actions
            />
          ))}

        {pathname === '/[projectId]/datasets/[datasetId]/labels' && (
          <DatasetLabels />
        )}

        {pathname === '/[projectId]/datasets/[datasetId]/models' && (
          <DatasetModels />
        )}
      </SuspenseBoundary>
    </>
  )
}

export default Dataset
