import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import { bytesToSize } from '../Bytes/helpers'

import Form from '../Form'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FileSvg from '../Icons/file.svg'
import WarningSvg from '../Icons/warning.svg'

const ModelUploadProgress = ({ state, dispatch }) => {
  return (
    <Form>
      <div css={{ color: colors.structure.zinc }}>Uploading:</div>

      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          paddingTop: spacing.normal,
          paddingBottom: spacing.comfy,
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

      <div css={{ paddingBottom: spacing.normal }}>
        <div
          css={{
            width: 500,
            height: 16,
            borderRadius: constants.borderRadius.large,
            backgroundColor: colors.structure.iron,
            overflow: 'hidden',
          }}
        >
          <div
            css={
              state.hasFailed
                ? {
                    width: (500 * state.progress) / 100,
                    height: 16,
                    backgroundColor: colors.signal.warning.base,
                  }
                : {
                    width: (500 * state.progress) / 100,
                    height: 16,
                    backgroundColor: colors.signal.grass.base,
                    transition: 'width 1s',
                  }
            }
          />
        </div>

        {state.hasFailed ? (
          <div
            css={{
              color: colors.signal.warning.base,
              display: 'flex',
              fontFamily: typography.family.condensed,
              paddingTop: spacing.base,
            }}
          >
            <WarningSvg height={constants.icons.small} />
            <div css={{ paddingLeft: spacing.base }}>Upload Failed</div>
          </div>
        ) : (
          <div
            css={{
              color: colors.signal.grass.base,
              display: 'flex',
              fontFamily: typography.family.condensed,
              paddingTop: spacing.base,
            }}
          >
            {Math.trunc(state.progress)}% Complete
          </div>
        )}
      </div>

      <ButtonGroup>
        {state.progress === 100 ? (
          <Button variant={BUTTON_VARIANTS.SECONDARY} isDisabled>
            Finishing...
          </Button>
        ) : (
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              state.request.abort()

              dispatch({
                file: undefined,
                isConfirmed: false,
                progress: 0,
                hasFailed: false,
                request: undefined,
              })
            }}
          >
            {state.hasFailed ? 'Retry' : 'Cancel'}
          </Button>
        )}
      </ButtonGroup>
    </Form>
  )
}

ModelUploadProgress.propTypes = {
  state: PropTypes.shape({
    file: PropTypes.shape({
      name: PropTypes.string.isRequired,
      size: PropTypes.number.isRequired,
    }).isRequired,
    isConfirmed: PropTypes.bool.isRequired,
    progress: PropTypes.number.isRequired,
    hasFailed: PropTypes.bool.isRequired,
    request: PropTypes.shape({
      abort: PropTypes.func.isRequired,
    }).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelUploadProgress
