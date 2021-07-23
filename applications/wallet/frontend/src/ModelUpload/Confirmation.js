import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import { bytesToSize } from '../Bytes/helpers'

import Form from '../Form'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FileSvg from '../Icons/file.svg'

import { onSubmit } from './helpers'

const ModelUploadConfirmation = ({ projectId, modelId, state, dispatch }) => {
  return (
    <Form>
      <div css={{ color: colors.structure.zinc }}>
        Do you want to upload this file?
      </div>

      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        <FileSvg height={33} />

        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            paddingLeft: spacing.normal,
          }}
        >
          <div
            css={{
              flex: 1,
              fontSize: typography.size.medium,
              lineHeight: typography.height.medium,
              paddingBottom: spacing.small,
            }}
          >
            {state.file.name}
          </div>

          <div
            css={{
              color: colors.structure.steel,
              fontFamily: typography.family.condensed,
            }}
          >
            {bytesToSize({ bytes: state.file.size })}
          </div>
        </div>
      </div>

      <ButtonGroup>
        <Button
          variant={BUTTON_VARIANTS.SECONDARY}
          onClick={() => {
            dispatch({ file: undefined })
          }}
        >
          Cancel
        </Button>

        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => {
            onSubmit({ projectId, modelId, state, dispatch })
          }}
        >
          Upload Model File
        </Button>
      </ButtonGroup>
    </Form>
  )
}

ModelUploadConfirmation.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  state: PropTypes.shape({
    file: PropTypes.shape({
      name: PropTypes.string.isRequired,
      size: PropTypes.number.isRequired,
    }).isRequired,
    isConfirmed: PropTypes.bool.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelUploadConfirmation
