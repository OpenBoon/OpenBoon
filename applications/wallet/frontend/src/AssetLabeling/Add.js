import { useReducer } from 'react'
import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

const HEIGHT = 40
const ICON_SIZE = 20

const noop = () => {}

const INITIAL_STATE = {
  model: '',
  label: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId }) => {
  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <div css={{ padding: spacing.normal }}>
      <Form style={{ width: '100%', padding: 0 }}>
        <label
          htmlFor="models-list"
          css={{ paddingBottom: spacing.base, color: colors.structure.zinc }}
        >
          Model:
        </label>
        <div css={{ paddingBottom: spacing.base }}>
          <select
            name="models-list"
            id="models-list"
            defaultValue=""
            onChange={({ target: { value } }) => {
              dispatch({ model: value })
            }}
            css={{
              backgroundColor: colors.structure.steel,
              borderRadius: constants.borderRadius.small,
              border: 'none',
              width: '100%',
              height: HEIGHT,
              color: colors.structure.white,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              paddingLeft: spacing.moderate,
              MozAppearance: 'none',
              WebkitAppearance: 'none',
              backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
              backgroundRepeat: `no-repeat, repeat`,
              backgroundPosition: `right ${spacing.base}px top 50%`,
              backgroundSize: ICON_SIZE,
            }}
          >
            <option value="" disabled>
              Select a model...
            </option>
            {models.map((option) => {
              return (
                <option key={option.name} value={option.id}>
                  {option.name}
                </option>
              )
            })}
          </select>
        </div>

        <Input
          id="asset-label"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Label:"
          type="text"
          value={state.label}
          onChange={({ target: { value } }) => dispatch({ label: value })}
          hasError={state.errors.label !== undefined}
          errorMessage={state.errors.label}
          style={{ width: '100%' }}
        />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={noop}
            style={{ flex: 1, margin: 0 }}
          >
            Cancel
          </Button>
          <div css={{ padding: spacing.base }} />
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={noop}
            isDisabled={!state.label || state.isLoading}
            style={{ flex: 1, margin: 0 }}
          >
            Save Label
          </Button>
        </div>
      </Form>
    </div>
  )
}

AssetLabelingAdd.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default AssetLabelingAdd
