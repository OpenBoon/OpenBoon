import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

const ApiKeys = () => {
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
    </div>
  )
}

export default ApiKeys
