import PropTypes from 'prop-types'
import { useReducer } from 'react'
import useSWR from 'swr'

import { colors, spacing } from '../Styles'

import FlashMessageErrors from '../FlashMessage/Errors'
import Form from '../Form'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Combobox from '../Combobox'

import { onSave, getSaveButtonCopy } from './helpers'

const BBOX_SIZE = 64

const INITIAL_STATE = ({ labels }) => ({
  labels,
  isChanged: false,
  isLoading: false,
  errors: { labels: {} },
})

const reducer = (state, action) => ({ ...state, ...action })

let reloadKey = 0

const FaceLabelingForm = ({ projectId, assetId, predictions }) => {
  const initializedState = INITIAL_STATE({
    labels: predictions.reduce((acc, { simhash, label }) => {
      return { ...acc, [simhash]: label }
    }, {}),
  })

  const [state, dispatch] = useReducer(reducer, initializedState)

  const { data } = useSWR(`/api/v1/projects/${projectId}/faces/labels/`)

  const { possibleLabels = [] } = data || {}

  const { isChanged, isLoading } = state

  return (
    <>
      <FlashMessageErrors
        errors={state.errors}
        styles={{
          paddingTop: spacing.base,
          paddingLeft: spacing.normal,
          paddingRight: spacing.normal,
          paddingBottom: spacing.comfy,
        }}
      />
      <Form
        style={{
          padding: 0,
          width: '100%',
          overflow: 'hidden',
        }}
      >
        <div
          css={{
            padding: spacing.normal,
            backgroundColor: colors.structure.coal,
            flex: 1,
            overflow: 'auto',
          }}
        >
          {predictions.map(({ simhash, bbox, b64Image, label }) => {
            return (
              <div
                key={simhash}
                css={{
                  display: 'flex',
                  alignItems: 'center',
                  paddingBottom: spacing.normal,
                }}
              >
                <img
                  css={{
                    maxHeight: BBOX_SIZE,
                    width: BBOX_SIZE,
                    objectFit: 'contain',
                    marginRight: spacing.normal,
                  }}
                  alt={bbox}
                  title={bbox}
                  src={b64Image}
                />
                <Combobox
                  key={reloadKey}
                  label="Name"
                  options={possibleLabels}
                  value={state.labels[simhash]}
                  onChange={({ value }) => {
                    return dispatch({
                      isChanged: value !== label,
                      labels: {
                        ...state.labels,
                        [simhash]: value,
                      },
                    })
                  }}
                  hasError={state.errors.labels[simhash] !== undefined}
                  errorMessage={state.errors.labels[simhash]}
                />
              </div>
            )
          })}
        </div>

        <div css={{ padding: spacing.base, display: 'flex', flexShrink: 0 }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              reloadKey += 1
              dispatch(initializedState)
            }}
            style={{ flex: 1 }}
            isDisabled={!isChanged || isLoading}
          >
            Cancel
          </Button>

          <div css={{ width: spacing.base, minWidth: spacing.base }} />

          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => {
              reloadKey += 1
              onSave({
                projectId,
                assetId,
                labels: state.labels,
                predictions,
                dispatch,
              })
            }}
            style={{ flex: 1 }}
            isDisabled={!isChanged || isLoading}
          >
            {getSaveButtonCopy({ isChanged, isLoading })}
          </Button>
        </div>
      </Form>
    </>
  )
}

FaceLabelingForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(
    PropTypes.shape({ simhash: PropTypes.string.isRequired }),
  ).isRequired,
}

export default FaceLabelingForm
