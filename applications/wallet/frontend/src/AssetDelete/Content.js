import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import Button, { VARIANTS } from '../Button'
import Radio from '../Radio'

import AssetDeleteConfirm from './Confirm'

export const noop = () => {}

const AssetDeleteContent = ({ showDialogue, setShowDialogue }) => {
  const {
    query,
    query: { projectId, id: assetId },
  } = useRouter()

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
        query={query}
        filename={filename}
        showDialogue={showDialogue}
        setShowDialogue={setShowDialogue}
      />
    )
  }

  return (
    <Form style={{ padding: spacing.normal, width: '100%' }}>
      <Radio
        option={{
          label: 'Delete Selected: 1',
          value: 'deleteSelected',
          initialValue: true,
          isDisabled: true,
        }}
        onClick={noop}
      />

      <div css={{ paddingLeft: spacing.comfy }}>Delete the selected asset</div>

      <div css={{ height: spacing.normal }} />

      <Button
        aria-label="Delete Asset"
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
  showDialogue: PropTypes.bool.isRequired,
  setShowDialogue: PropTypes.func.isRequired,
}

export default AssetDeleteContent
