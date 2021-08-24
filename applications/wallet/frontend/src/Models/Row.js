import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, constants } from '../Styles'

import { onRowClickRouterPush } from '../Table/helpers'

import WarningSvg from '../Icons/warning.svg'
import GeneratingSvg from '../Icons/generating.svg'

import ModelsMenu from './Menu'

const DEPLOYING = 'Deploying'
const DEPLOY_ERROR = 'DeployError'

const ModelsRow = ({
  projectId,
  model: { id: modelId, name, type, moduleName, state },
  modelTypes,
}) => {
  const { label } = modelTypes.find(({ name: n }) => n === type) || {
    label: type,
  }

  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/[projectId]/models/[modelId]',
        `/${projectId}/models/${modelId}`,
      )}
    >
      <td>
        <Link
          href="/[projectId]/models/[modelId]"
          as={`/${projectId}/models/${modelId}`}
          passHref
        >
          <a css={{ display: 'flex', ':hover': { textDecoration: 'none' } }}>
            {state === DEPLOY_ERROR && (
              <WarningSvg
                height={20}
                color={colors.signal.warning.base}
                css={{ marginRight: spacing.base }}
              />
            )}

            {state === DEPLOYING && (
              <GeneratingSvg
                height={constants.icons.regular}
                color={colors.signal.canary.base}
                css={{
                  marginRight: spacing.base,
                  animation: constants.animations.infiniteRotation,
                }}
              />
            )}

            {name}
          </a>
        </Link>
      </td>

      <td>{label}</td>

      <td>{moduleName}</td>

      <td>
        <ModelsMenu projectId={projectId} modelId={modelId} name={name} />
      </td>
    </tr>
  )
}

ModelsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    moduleName: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
  }).isRequired,
  modelTypes: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ModelsRow
