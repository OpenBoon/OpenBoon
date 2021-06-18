import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import Button, { VARIANTS } from '../Button'
import Radio from '../Radio'

import AssetDeleteConfirm from './Confirm'

const noop = () => () => {}

const AssetDeleteContent = ({
  projectId,
  assetId,
  showDialogue,
  setShowDialogue,
}) => {
  const {
    data: {
      metadata: {
        source: { filename },
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  if (showDialogue) {
    return (
      <AssetDeleteConfirm
        filename={filename}
        showDialogue={showDialogue}
        setShowDialogue={setShowDialogue}
      />
    )
  }

  return (
    <Form style={{ padding: spacing.normal, width: '100%' }}>
      <Radio
        name="delete"
        option={{
          value: 'deleteSelected',
          label: 'Delete Selected: 1',
          legend: 'Delete the selected asset',
          initialValue: true,
        }}
        onClick={noop}
      />

      <div css={{ height: spacing.normal }} />

      <Button
        variant={VARIANTS.PRIMARY_SMALL}
        style={{ width: 'fit-content' }}
        onClick={() => {
          setShowDialogue(true)
        }}
      >
        Delete Asset
      </Button>
    </Form>
  )
}

AssetDeleteContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  showDialogue: PropTypes.bool.isRequired,
  setShowDialogue: PropTypes.func.isRequired,
}

export default AssetDeleteContent
