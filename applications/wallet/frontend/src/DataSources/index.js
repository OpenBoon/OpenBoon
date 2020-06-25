import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'
import { spacing } from '../Styles'

import DataSourcesRow from './Row'

const DataSources = () => {
  const {
    query: { projectId, action },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Data Sources</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      {action === 'add-datasource-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={VARIANTS.SUCCESS}>
            Data source created.
          </FlashMessage>
        </div>
      )}

      {action === 'edit-datasource-success' && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={VARIANTS.SUCCESS}>
            Data source edited.
          </FlashMessage>
        </div>
      )}

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/data-sources' },
          {
            title: 'Create Data Source',
            href: '/[projectId]/data-sources/add',
          },
        ]}
      />

      <Table
        role={ROLES.ML_Tools}
        legend="Sources"
        url={`/api/v1/projects/${projectId}/data_sources/`}
        refreshKeys={[]}
        columns={[
          'Name',
          'Source Type',
          'Path',
          'Date Created',
          'Date Modified',
          'File Types',
          '#Actions#',
        ]}
        expandColumn={0}
        renderEmpty="No data sources"
        renderRow={({ result, revalidate }) => (
          <DataSourcesRow
            key={result.id}
            projectId={projectId}
            dataSource={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default DataSources
