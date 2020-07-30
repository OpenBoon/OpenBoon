import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'
import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'

const AssetLabelingMenu = ({
  projectId,
  queryString,
  modelId,
  label,
  triggerReload,
}) => {
  const [, setLocalModel] = useLocalStorageState({
    key: 'AssetLabelingAdd.Model',
  })
  const [, setLocalLabel] = useLocalStorageState({
    key: 'AssetLabelingAdd.Label',
  })

  return (
    <Menu
      open="left"
      button={ButtonActions}
      style={{ color: colors.structure.steel }}
    >
      {({ onClick }) => (
        <ul>
          <li>
            <Link
              href="/[projectId]/models/[modelId]"
              as={`/${projectId}/models/${modelId}`}
              passHref
            >
              <Button variant={VARIANTS.MENU_ITEM}>
                View Model/Train & Apply
              </Button>
            </Link>
          </li>
          <li>
            <Link
              href={`/[projectId]/visualizer${queryString}`}
              as={`/${projectId}/visualizer/${queryString}`}
              passHref
            >
              <Button variant={VARIANTS.MENU_ITEM}>Add Label Filter</Button>
            </Link>
          </li>
          <li>
            <Button
              variant={VARIANTS.MENU_ITEM}
              onClick={() => {
                setLocalModel({ value: modelId })
                setLocalLabel({ value: label })
                triggerReload()
                onClick()
              }}
              isDisabled={false}
            >
              Edit Label
            </Button>
          </li>
        </ul>
      )}
    </Menu>
  )
}

AssetLabelingMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  queryString: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  triggerReload: PropTypes.func.isRequired,
}

export default AssetLabelingMenu
