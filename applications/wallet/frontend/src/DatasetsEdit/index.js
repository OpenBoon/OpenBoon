import Head from 'next/head'
import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import DatasetDetails from '../Dataset/Details'
import ItemSeparator from '../Item/Separator'
import DatasetsEditForm from './Form'

const DatasetsEdit = () => {
  const {
    pathname,
    query: { projectId, datasetId },
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

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <DatasetDetails
          key={pathname}
          projectId={projectId}
          datasetId={datasetId}
        />

        <div css={{ height: spacing.normal }} />

        <ItemSeparator />

        <DatasetsEditForm />
      </SuspenseBoundary>
    </>
  )
}

export default DatasetsEdit
