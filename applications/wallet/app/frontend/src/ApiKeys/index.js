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
    <div>
      <Head>
        <title>API Keys</title>
      </Head>

      <PageTitle>Project API Keys</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/api-keys' },
          { title: 'Create API key', href: '/api-keys/add' },
        ]}
      />

      <Table
        url={`/api/v1/projects/${projectId}/apikeys/`}
        columns={['API Key Name', 'Permissions']}
        renderRow={({ result }) => (
          <ApiKeysRow key={result.id} apiKey={result} />
        )}
      />
    </div>
  )
}

export default ApiKeys
