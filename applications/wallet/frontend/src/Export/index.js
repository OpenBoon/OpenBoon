import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const Export = () => {
  return (
    <div css={{ padding: spacing.base }}>
      Export the metadata for all assets in the current search as CSV. File will
      download automatically.
      <div css={{ height: spacing.base }} />
      <Button variant={VARIANTS.PRIMARY_SMALL} onClick={console.warn}>
        Export CSV
      </Button>
    </div>
  )
}

export default Export
