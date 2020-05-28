import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Form from '../Form'
import Button, { VARIANTS } from '../Button'
import Radio from '../Radio'

const AssetDeleteContent = ({ setShowDialogue }) => {
  return (
    <Form style={{ padding: spacing.normal, width: '100%' }}>
      <Radio
        option={{
          label: 'Delete Selected: 1',
          value: 'deleteSelected',
          initialValue: true,
          isDisabled: true,
        }}
        onClick={() => {}}
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
  setShowDialogue: PropTypes.func.isRequired,
}

export default AssetDeleteContent
