import { useReducer } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import Form from '../Form'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'

const MODULE = 'zvi-face-detection'
const BBOX_SIZE = 64

const INITIAL_STATE = ({ labels }) => ({
  labels,
  isChanged: false,
  isLoading: false,
  errors: { labels: {} },
})

const reducer = (state, action) => ({ ...state, ...action })

const FaceLabelingContent = ({ projectId, assetId }) => {
  const { data: asset } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/`,
  )

  const {
    metadata: {
      source: { filename },
    },
  } = asset

  const attr = `analysis.${MODULE}&width=${BBOX_SIZE}`

  const {
    data: { [MODULE]: module },
  } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  const { count, predictions = [] } = module || {}

  const initializedState = INITIAL_STATE({
    labels: predictions.reduce((acc, { simhash, label }) => {
      return { ...acc, [simhash]: label }
    }, {}),
  })

  const [state, dispatch] = useReducer(reducer, initializedState)

  if (!count || count < 1) {
    return (
      <div css={{ padding: spacing.normal, color: colors.structure.white }}>
        No faces have been detected in this asset for naming and training.
        Please select another asset.
      </div>
    )
  }

  return (
    <>
      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.divider,
        }}
      >
        <span>
          Once a name has been added to a face, training can begin. Names can
          continue to be edited as needed.
        </span>

        <div css={{ height: spacing.normal }} />

        <Button
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={console.warn}
          isDisabled
        >
          Train &amp; Apply
        </Button>
      </div>

      <div
        css={{
          padding: spacing.normal,
          color: colors.signal.sky.base,
        }}
      >
        {filename}
      </div>

      <Form
        style={{
          padding: spacing.normal,
          width: '100%',
          backgroundColor: colors.structure.coal,
        }}
      >
        <div>
          {predictions.map((prediction) => {
            return (
              <div
                key={prediction.simhash}
                css={{ display: 'flex', alignItems: 'center' }}
              >
                <img
                  css={{
                    maxHeight: BBOX_SIZE,
                    width: BBOX_SIZE,
                    objectFit: 'contain',
                    marginRight: spacing.normal,
                  }}
                  alt={prediction.bbox}
                  title={prediction.bbox}
                  src={prediction.b64_image}
                />

                <Input
                  id={prediction.simhash}
                  variant={INPUT_VARIANTS.SECONDARY}
                  label="Name:"
                  type="text"
                  style={{ flex: 1 }}
                  value={state.labels[prediction.simhash]}
                  onChange={({ target: { value } }) =>
                    dispatch({
                      isChanged: true,
                      labels: {
                        ...state.labels,
                        [prediction.simhash]: value,
                      },
                    })
                  }
                  hasError={
                    state.errors.labels[prediction.simhash] !== undefined
                  }
                  errorMessage={state.errors.labels[prediction.simhash]}
                />
              </div>
            )
          })}
        </div>
        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              dispatch(initializedState)
            }}
            style={{ flex: 1 }}
            isDisabled={!state.isChanged}
          >
            Cancel
          </Button>

          <div css={{ width: spacing.base, minWidth: spacing.base }} />

          <Button
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={console.warn}
            style={{ flex: 1 }}
            isDisabled={!state.isChanged}
          >
            {state.isChanged ? 'Save' : 'Saved'}
          </Button>
        </div>
      </Form>
    </>
  )
}

FaceLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default FaceLabelingContent
