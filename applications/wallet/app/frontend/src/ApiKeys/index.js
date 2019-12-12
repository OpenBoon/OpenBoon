import { spacing } from '../Styles'

import Tabs from '../Tabs'

const ApiKeys = () => {
  return (
    <div css={{ padding: spacing.normal }}>
      <h2>Project API Keys</h2>
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
