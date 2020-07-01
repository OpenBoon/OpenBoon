import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'
import Table, { ROLES } from '../Table'

import ApiKeysCopy from './Copy'
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

      <ApiKeysCopy />

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/api-keys' },
          { title: 'Create API key', href: '/[projectId]/api-keys/add' },
        ]}
      />

      <Table
        role={ROLES.API_Keys}
        legend="Keys"
        url={`/api/v1/projects/${projectId}/api_keys/`}
        refreshKeys={[]}
        refreshButton={false}
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
