import { Checkbox, FormControlLabel } from "@material-ui/core";
import { OrderType } from "../orderTypes/typed";

interface Props {
  orderType: OrderType;
  checked: boolean;
  onChange: (id: string, evt: any) => void;
}

const ManagedOrderType: React.FC<Props> = ({ orderType, checked, onChange }) => {
  return (
    <FormControlLabel
      control={
        <Checkbox
          checked={checked}
          onChange={evt => {
            onChange(orderType.id as string, evt.target.checked);
          }}
          name={`check-orderType-${orderType.id}`}
        />
      }
      label={orderType.descrizione}
    />
  );
};

export default ManagedOrderType