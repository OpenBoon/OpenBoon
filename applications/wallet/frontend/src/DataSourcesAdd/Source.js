import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'

const WIDTH = 300
const HEIGHT = 40
const CHEVRON_HEIGHT = 20

export const SOURCES = {
  AWS: {
    label: 'Amazon Web Service (AWS)',
    uri: 's3://',
    credentials: [
      {
        key: 'aws_access_key_id',
        label: 'AWS Access Key ID',
        isRequired: true,
      },
      {
        key: 'aws_secret_access_key',
        label: 'AWS Secret Access Key',
        isRequired: true,
      },
    ],
  },
  AZURE: {
    label: 'Azure',
    uri: 'azure://',
    credentials: [
      {
        key: 'connection_string',
        label: 'Connection String',
        isRequired: true,
      },
    ],
  },
  GCP: {
    label: 'Google Cloud Platform (GCP)',
    uri: 'gs://',
    credentials: [
      {
        key: 'service_account_json_key',
        label: 'Service Account JSON Key',
        isRequired: false,
      },
    ],
  },
}

const DataSourcesAddSource = ({
  dispatch,
  state: { credentials, errors: stateErrors, source, uri },
}) => {
  return (
    <div css={{ paddingTop: spacing.base }}>
      <label
        htmlFor="source-selection"
        css={{ paddingBottom: spacing.base, color: colors.structure.zinc }}
      >
        Source type <span css={{ color: colors.signal.warning.base }}>*</span>
      </label>
      <div css={{ paddingTop: spacing.small }}>
        <select
          name="sources"
          id="source-selection"
          onChange={({ target: { value } }) => {
            const requiredCredentials = SOURCES[value].credentials.reduce(
              (acc, cred) => {
                const { key, isRequired } = cred
                acc[key] = { value: '', isRequired }
                return acc
              },
              {},
            )

            return dispatch({
              source: value,
              uri: SOURCES[value].uri,
              credentials: {
                ...credentials,
                [value]: { ...requiredCredentials },
              },
              errors: { ...stateErrors, uri: '' },
            })
          }}
          css={{
            backgroundColor: colors.structure.steel,
            borderRadius: constants.borderRadius.small,
            border: 'none',
            width: WIDTH,
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
            backgroundSize: CHEVRON_HEIGHT,
          }}
        >
          <option value="" disabled selected>
            Select source
          </option>
          {Object.keys(SOURCES).map((option) => {
            return (
              <option key={option} value={option}>
                {SOURCES[option].label}
              </option>
            )
          })}
        </select>
      </div>
      &nbsp;
      {source && (
        <>
          <Input
            id="uri"
            variant={INPUT_VARIANTS.SECONDARY}
            label="Storage Address"
            type="text"
            value={uri}
            onChange={({ target: { value } }) => {
              const uriPrefix = value.match(/(\w.*:\/{2})/)
              const hasError =
                !uriPrefix ||
                value === SOURCES[source].uri ||
                (uriPrefix && uriPrefix[0] !== SOURCES[source].uri)
              const errorMessage =
                value === SOURCES[source].uri
                  ? `Storage address is incomplete`
                  : `${SOURCES[source].label} address must begin with ${SOURCES[source].uri}`
              return dispatch({
                uri: value,
                errors: { ...stateErrors, uri: hasError ? errorMessage : '' },
              })
            }}
            hasError={!!stateErrors.uri}
            errorMessage={stateErrors.uri}
            isRequired
          />
          {SOURCES[source].credentials.map(({ key, label, isRequired }) => {
            return (
              <Input
                key={key}
                id={key}
                variant={INPUT_VARIANTS.SECONDARY}
                label={label}
                type="text"
                value={credentials[source][key].value}
                onChange={({ target: { value } }) => {
                  const errorMessage =
                    isRequired && value === ''
                      ? `${label} must not be empty`
                      : ''

                  return dispatch({
                    credentials: {
                      ...credentials,
                      [source]: {
                        ...credentials[source],
                        [key]: { value, isRequired },
                      },
                    },
                    errors: {
                      ...stateErrors,
                      [source]: {
                        ...stateErrors[source],
                        [key]: errorMessage,
                      },
                    },
                  })
                }}
                hasError={
                  stateErrors[source] ? !!stateErrors[source][key] : false
                }
                errorMessage={stateErrors[source] && stateErrors[source][key]}
                isRequired={isRequired}
              />
            )
          })}
        </>
      )}
    </div>
  )
}

DataSourcesAddSource.propTypes = {
  dispatch: PropTypes.func.isRequired,
  state: PropTypes.shape({
    errors: PropTypes.shape({
      credential: PropTypes.string,
      keyId: PropTypes.string,
      uri: PropTypes.string,
    }).isRequired,
    credentials: PropTypes.shape({}).isRequired,
    source: PropTypes.string.isRequired,
    uri: PropTypes.string.isRequired,
  }).isRequired,
}

export default DataSourcesAddSource
