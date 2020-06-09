import { useRouter } from 'next/router'

import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const Export = () => {
  const {
    query: { projectId, query = 'W10=' },
  } = useRouter()

  return (
    <div css={{ padding: spacing.normal }}>
      <div>
        Export the metadata for all assets in the current search as a CSV file. File
        will download automatically.
      </div>

      <div css={{ height: spacing.normal }} />

      <Button
        variant={VARIANTS.PRIMARY_SMALL}
        href={`/api/v1/projects/${projectId}/searches/export/?query=${query}&format=csv`}
        style={{ width: 'fit-content' }}
      >
        Export CSV
      </Button>
    </div>
  )
}

export default Export
