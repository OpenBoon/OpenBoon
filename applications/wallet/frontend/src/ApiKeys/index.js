import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import Table from '../Table'

import ApiKeysRow from './Row'

const ApiKeys = () => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>API Keys</title>
      </Head>

      <PageTitle>Project API Keys</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/api-keys' },
          { title: 'Create API key', href: '/[projectId]/api-keys/add' },
        ]}
      />

      <Table
        assetType="Keys"
        url={`/api/v1/projects/${projectId}/apikeys/`}
        columns={['API Key Name', 'Permissions', '#Actions#']}
        expandColumn={2}
        renderEmpty="No api keys"
        renderRow={({ result, revalidate }) => (
          <ApiKeysRow
            key={result.id}
            projectId={projectId}
            apiKey={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default ApiKeys
