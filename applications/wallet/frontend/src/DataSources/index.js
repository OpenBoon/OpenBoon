import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import PageTitle from '../PageTitle'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'

import DataSourcesRow from './Row'

const DataSources = () => {
  const {
    query: { projectId, action, jobId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Data Sources</title>
      </Head>

      <PageTitle>Data Sources</PageTitle>

      {!!action && (
        <div css={{ display: 'flex', paddingTop: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Data source
            {action === 'add-datasource-success' && ' created'}
            {action === 'scan-datasource-success' && ' is being scanned'}
            {action === 'delete-datasource-success' && ' deleted'}
            {action === 'edit-datasource-success' && ' edited'}.{' '}
            {!!jobId && (
              <Link
                href="/[projectId]/jobs/[jobId]"
                as={`/${projectId}/jobs/${jobId}`}
                passHref
              >
                <a>Check Job Status</a>
              </Link>
            )}
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
        refreshButton={false}
        columns={[
          'Name',
          'Source Type',
          'Path',
          'Date Created',
          'Date Modified',
          'File Types',
          '#Actions#',
        ]}
        expandColumn={3}
        renderEmpty="There are currently no data sources."
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
